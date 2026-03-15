package com.esign.service.impl;

import com.esign.constant.FolderPermissionType;
import com.esign.constant.StatusMessage;
import com.esign.entities.folder.*;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.AuthService;
import com.esign.service.FolderService;
import com.esign.specification.FolderSpecification;
import com.esign.specification.FolderTrashSpecification;
import com.esign.specification.SubFolderSpecification;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FolderContributorRepository folderContributorRepository;
    private final AuthService authService;
    private final UserRoleRepository userRoleRepository;
    private final ValidationUtil validationUtil;
    private final UserRepository userRepository;
    private final FolderSpecification folderSpecification;
    private final SubFolderSpecification subFolderSpecification;
    private final FolderTrashSpecification folderTrashSpecification;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse create(FolderRequest request) throws BadRequestException {
        validationUtil.validate(request);
        User owner = authService.getAuthenticatedUser();

        Folder parent;
        if (request.getParentId() != null) {
            parent = findFolderById(request.getParentId());

            // public folder → semua eligible user bisa buat sub folder
            // private folder → harus punya MANAGE permission
            if (!parent.getIsPublic()) {
                validateAccess(parent, owner, FolderPermissionType.MANAGE);
            } else {
                // tetap validasi role jika folder role-restricted
                if (parent.getRequiredRole() != null) {
                    boolean hasRole = userRoleRepository.findByUser(owner)
                            .stream()
                            .anyMatch(ur -> ur.getRole().getId()
                                    .equals(parent.getRequiredRole().getId()));
                    if (!hasRole) {
                        throw new AccessDeniedException("You don't have access to this folder");
                    }
                }
            }

            boolean parentIsRole = parent.getRequiredRole() != null;
            boolean childIsRole = request.getIsRoleRestricted();

            if (parentIsRole && !childIsRole) {
                throw new BadRequestException("Cannot create public folder inside role-restricted folder");
            }
            if (!parentIsRole && childIsRole) {
                throw new BadRequestException("Cannot create role-restricted folder inside public folder");
            }
        } else {
            parent = null;
        }

        if (folderRepository.existsByNameAndParentAndIsDeletedFalse(request.getName(), parent)) {
            throw new BadRequestException("Folder name already exists in this location");
        }

        Role requiredRole = null;
        if (request.getIsRoleRestricted()) {
            if (parent != null && parent.getRequiredRole() != null) {
                requiredRole = parent.getRequiredRole();
            } else {
                requiredRole = userRoleRepository.findByUser(owner)
                        .stream()
                        .findFirst()
                        .map(UserRole::getRole)
                        .orElseThrow(() -> new BadRequestException("User has no role"));
            }
        }

        Folder folder = folderRepository.save(Folder.builder()
                .name(request.getName())
                .parent(parent)
                .owner(owner)
                .requiredRole(requiredRole)
                .isPublic(request.getIsPublic())
                .build());

        folderContributorRepository.save(FolderContributor.builder()
                .folder(folder)
                .user(owner)
                .permissionType(FolderPermissionType.MANAGE)
                .build());

        return toResponse(folder);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FolderResponse> getRootFolders(SearchFolderRequest request) {
        User user = authService.getAuthenticatedUser();

        List<String> userRoleIds = userRoleRepository.findByUser(user)
                .stream()
                .map(ur -> ur.getRole().getId())
                .toList();

        Specification<Folder> spec = folderSpecification.specification(request, user, userRoleIds);

        return folderRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public FolderResponse getById(String id, SearchSubFolderRequest request) {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, null);

        // ambil sub folder dengan filter
        List<FolderResponse> children = folderRepository
                .findAll(subFolderSpecification.specification(request, user, folder))
                .stream()
                .map(this::toResponse)
                .toList();

        FolderResponse response = toResponse(folder);
        response.setChildren(children);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse rename(String id, FolderRenameRequest request) throws BadRequestException {
        validationUtil.validate(request);
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, FolderPermissionType.MANAGE);

        if (folderRepository.existsByNameAndParentAndIsDeletedFalse(request.getName(), folder.getParent())) {
            throw new BadRequestException("Folder name already exists in this location");
        }

        folder.setName(request.getName());
        return toResponse(folderRepository.save(folder));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse move(String id, FolderMoveRequest request) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateOwner(folder, user);

        Folder newParent = null;
        if (request.getParentId() != null) {
            newParent = findFolderById(request.getParentId());

            // tidak bisa pindah ke diri sendiri
            if (newParent.getId().equals(folder.getId())) {
                throw new BadRequestException("Cannot move folder to itself");
            }

            // tidak bisa pindah ke child folder sendiri
            if (isDescendant(folder, newParent)) {
                throw new BadRequestException("Cannot move folder to its own sub folder");
            }

            // validasi konsistensi role
            boolean newParentIsRole = newParent.getRequiredRole() != null;
            boolean folderIsRole = folder.getRequiredRole() != null;

            if (newParentIsRole && !folderIsRole) {
                throw new BadRequestException("Cannot move public folder into role-restricted folder");
            }
            if (!newParentIsRole && folderIsRole) {
                throw new BadRequestException("Cannot move role-restricted folder into public folder");
            }

            // validasi akses ke parent baru
            if (!newParent.getIsPublic()) {
                validateAccess(newParent, user, FolderPermissionType.MANAGE);
            }
        }

        // cek duplikat nama di lokasi baru
        if (folderRepository.existsByNameAndParentAndIsDeletedFalse(folder.getName(), newParent)) {
            throw new BadRequestException("Folder name already exists in destination location");
        }

        folder.setParent(newParent);
        return toResponse(folderRepository.save(folder));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse toggleVisibility(String id) {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateOwner(folder, user);

        folder.setIsPublic(!folder.getIsPublic());
        return toResponse(folderRepository.save(folder));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addContributor(String id, FolderContributorRequest request) throws BadRequestException {
        validationUtil.validate(request);
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, FolderPermissionType.MANAGE);

        User targetUser = userRepository.findByIdAndIsEnableTrue(request.getUserId())
                .orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));

        // owner tidak bisa diubah permission-nya
        if (targetUser.getId().equals(folder.getOwner().getId())) {
            throw new BadRequestException("Cannot modify owner permission");
        }

        // validasi role jika folder role-restricted
        if (folder.getRequiredRole() != null) {
            boolean targetHasRole = userRoleRepository.findByUser(targetUser)
                    .stream()
                    .anyMatch(ur -> ur.getRole().getId()
                            .equals(folder.getRequiredRole().getId()));

            if (!targetHasRole) {
                throw new BadRequestException(
                        "User must have role " + folder.getRequiredRole().getName() + " to be added as contributor"
                );
            }
        }

        FolderContributor contributor = folderContributorRepository
                .findByFolderAndUser(folder, targetUser)
                .orElse(FolderContributor.builder()
                        .folder(folder)
                        .user(targetUser)
                        .build());

        contributor.setPermissionType(request.getPermissionType());
        folderContributorRepository.save(contributor);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeContributor(String id, String targetUserId) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, FolderPermissionType.MANAGE);

        User targetUser = userRepository.findByIdAndIsEnableTrue(targetUserId)
                .orElseThrow(() -> new NotFoundException(StatusMessage.USER_NOT_FOUND));

        // owner tidak bisa dihapus dari contributor
        if (targetUser.getId().equals(folder.getOwner().getId())) {
            throw new BadRequestException("Cannot remove owner from contributor");
        }

        folderContributorRepository.deleteByFolderAndUser(folder, targetUser);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String id) {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, FolderPermissionType.MANAGE);
        softDeleteRecursive(folder, user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FolderTrashResponse> getTrash(SearchFolderTrashRequest request) {
        User user = authService.getAuthenticatedUser();

        List<String> userRoleIds = userRoleRepository.findByUser(user)
                .stream()
                .map(ur -> ur.getRole().getId())
                .toList();

        return folderRepository
                .findAll(folderTrashSpecification.specification(request, user, userRoleIds))
                .stream()
                .map(folder -> toTrashResponse(folder, user))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse restore(String id) throws BadRequestException {
        User user = authService.getAuthenticatedUser();

        Folder folder = folderRepository.findByIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new NotFoundException("Folder not found in trash"));

        validateAccessForRestore(folder, user);

        if (folder.getOriginalParentId() != null) {
            Folder originalParent = folderRepository.findById(folder.getOriginalParentId())
                    .orElseThrow(() -> new BadRequestException("Cannot restore, parent folder has been permanently deleted"));

            if (originalParent.getIsDeleted()) {
                throw new BadRequestException("Cannot restore, parent folder '" + originalParent.getName() + "' is still in trash. Restore it first");
            }

            folder.setParent(originalParent);
        }

        restoreRecursive(folder, folder.getDeletedAt());
        return toResponse(folder);
    }

    private void validateOwner(Folder folder, User user) {
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only owner can delete this folder");
        }
    }

    // cek apakah target adalah descendant dari folder
    private boolean isDescendant(Folder folder, Folder target) {
        Folder current = target;
        while (current.getParent() != null) {
            if (current.getParent().getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void validateAccess(Folder folder, User user, FolderPermissionType required) {
        // 1. cek required_role
        if (folder.getRequiredRole() != null) {
            boolean hasRole = userRoleRepository.findByUser(user)
                    .stream()
                    .anyMatch(ur -> ur.getRole().getId()
                            .equals(folder.getRequiredRole().getId()));
            if (!hasRole) {
                throw new AccessDeniedException("You don't have access to this folder");
            }
        }

        // 2. owner selalu bisa akses
        if (folder.getOwner().getId().equals(user.getId())) return;

        // 3. public → semua user boleh view
        if (required == null && folder.getIsPublic()) return;

        // 4. cek contributor untuk UPLOAD/MANAGE
        FolderContributor contributor = folderContributorRepository
                .findByFolderAndUser(folder, user)
                .orElseThrow(() -> new AccessDeniedException("You don't have access to this folder"));

        if (required == null) return; // hanya VIEW, contributor terdaftar cukup

        boolean hasPermission = switch (required) {
            case UPLOAD -> contributor.getPermissionType() == FolderPermissionType.UPLOAD
                    || contributor.getPermissionType() == FolderPermissionType.MANAGE;
            case MANAGE -> contributor.getPermissionType() == FolderPermissionType.MANAGE;
        };

        if (!hasPermission) {
            throw new AccessDeniedException("You don't have " + required + " permission on this folder");
        }
    }

    private Folder findFolderById(String id) {
        return folderRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Folder not found"));
    }

    private FolderResponse toResponse(Folder folder) {
        List<FolderContributorResponse> contributors = folderContributorRepository
                .findAllByFolder(folder)
                .stream()
                .map(c -> FolderContributorResponse.builder()
                        .id(c.getId())
                        .userId(c.getUser().getId())
                        .username(c.getUser().getUsername())
                        .permissionType(c.getPermissionType())
                        .build())
                .toList();

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
                .isPublic(folder.getIsPublic())
                .requiredRole(folder.getRequiredRole() != null ? folder.getRequiredRole().getName() : null)
                .contributors(contributors)
                .createdAt(folder.getCreatedAt().toString())
                .updatedAt(folder.getUpdatedAt().toString())
                .build();
    }

    private void softDeleteRecursive(Folder folder, User deletedBy) {
        LocalDateTime now = LocalDateTime.now();
        softDeleteRecursiveWithTime(folder, deletedBy, now, true);
    }

    private void softDeleteRecursiveWithTime(Folder folder, User deletedBy, LocalDateTime deletedAt, boolean isDirect) {
        List<Folder> children = folderRepository.findAllByParentAndIsDeletedFalse(folder);
        for (Folder child : children) {
            softDeleteRecursiveWithTime(child, deletedBy, deletedAt, false);
        }
        folder.setOriginalParentId(
                folder.getParent() != null ? folder.getParent().getId() : null
        );
        folder.setIsDeleted(true);
        folder.setIsDirectDeleted(isDirect);
        folder.setDeletedAt(deletedAt);
        folder.setDeletedBy(deletedBy);
        folderRepository.save(folder);
    }

    private void restoreRecursive(Folder folder, LocalDateTime deletedAt) {
        folder.setIsDeleted(false);
        folder.setIsDirectDeleted(false);
        folder.setDeletedAt(null);
        folder.setDeletedBy(null);
        folder.setOriginalParentId(null);
        folderRepository.save(folder);

        folderRepository.findAllByOriginalParentIdAndIsDeletedTrue(folder.getId())
                .stream()
                .filter(child -> child.getDeletedAt().equals(deletedAt))
                .forEach(child -> restoreRecursive(child, deletedAt));
    }

    private void validateAccessForRestore(Folder folder, User user) {
        // cek role jika folder role-restricted
        if (folder.getRequiredRole() != null) {
            boolean hasRole = userRoleRepository.findByUser(user)
                    .stream()
                    .anyMatch(ur -> ur.getRole().getId()
                            .equals(folder.getRequiredRole().getId()));
            if (!hasRole) {
                throw new AccessDeniedException("You don't have access to restore this folder");
            }
        }

        // owner selalu bisa restore
        if (folder.getOwner().getId().equals(user.getId())) return;

        // cek contributor MANAGE
        FolderContributor contributor = folderContributorRepository
                .findByFolderAndUser(folder, user)
                .orElseThrow(() -> new AccessDeniedException("You don't have access to restore this folder"));

        if (contributor.getPermissionType() != FolderPermissionType.MANAGE) {
            throw new AccessDeniedException("Only MANAGE permission can restore folder");
        }
    }

    private FolderTrashResponse toTrashResponse(Folder folder, User currentUser) {
        boolean isOwner = folder.getOwner().getId().equals(currentUser.getId());
        boolean hasManage = isOwner || folderContributorRepository
                .findByFolderAndUser(folder, currentUser)
                .map(c -> c.getPermissionType() == FolderPermissionType.MANAGE)
                .orElse(false);

        boolean canRestore = false;
        String restoreNote = null;

        if (hasManage) {
            if (folder.getOriginalParentId() == null) {
                canRestore = true;
            } else {
                java.util.Optional<Folder> originalParent = folderRepository
                        .findById(folder.getOriginalParentId());

                if (originalParent.isEmpty()) {
                    canRestore = false;
                    restoreNote = "Cannot restore, parent folder has been permanently deleted";
                } else if (originalParent.get().getIsDeleted()) {
                    canRestore = false;
                    restoreNote = "Parent folder '" + originalParent.get().getName() + "' is still in trash, restore it first";
                } else {
                    canRestore = true;
                }
            }
        } else {
            restoreNote = "Only MANAGE permission can restore folder";
        }

        return FolderTrashResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .type(folder.getRequiredRole() != null ? "ROLE" : "PUBLIC")
                .requiredRole(folder.getRequiredRole() != null ? folder.getRequiredRole().getName() : null)
                .isPublic(folder.getIsPublic())
                .deletedAt(folder.getDeletedAt().toString())
                .deletedBy(folder.getDeletedBy().getUsername())
                .canRestore(canRestore)
                .restoreNote(restoreNote)
                .build();
    }
}
