package com.esign.service.impl;

import com.esign.constant.FolderPermissionType;
import com.esign.constant.StatusMessage;
import com.esign.entities.folder.FolderContributorRequest;
import com.esign.entities.folder.FolderContributorResponse;
import com.esign.entities.folder.FolderRequest;
import com.esign.entities.folder.FolderResponse;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.model.Folder;
import com.esign.model.FolderContributor;
import com.esign.model.Role;
import com.esign.model.User;
import com.esign.repository.*;
import com.esign.service.AuthService;
import com.esign.service.FolderService;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FolderContributorRepository folderContributorRepository;
    private final AuthService authService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final ValidationUtil validationUtil;
    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse create(FolderRequest request) throws BadRequestException, AccessDeniedException {
        validationUtil.validate(request);
        User owner = authService.getAuthenticatedUser();

        // cek parent folder
        Folder parent = null;
        if (request.getParentId() != null) {
            parent = findFolderById(request.getParentId());
            validateAccess(parent, owner, FolderPermissionType.MANAGE);
        }

        // cek duplikat nama
        if (folderRepository.existsByNameAndParentAndIsDeletedFalse(request.getName(), parent)) {
            throw new BadRequestException("Folder name already exists in this location");
        }

        // cek required role
        Role requiredRole = null;
        if (request.getRequiredRoleId() != null) {
            requiredRole = roleRepository.findByIdAndIsActiveTrue(request.getRequiredRoleId())
                    .orElseThrow(() -> new NotFoundException(StatusMessage.ROLE_NOT_FOUND));
        }

        Folder folder = folderRepository.save(Folder.builder()
                .name(request.getName())
                .parent(parent)
                .owner(owner)
                .requiredRole(requiredRole)
                .isPublic(request.getIsPublic())
                .build());

        // owner otomatis MANAGE
        folderContributorRepository.save(FolderContributor.builder()
                .folder(folder)
                .user(owner)
                .permissionType(FolderPermissionType.MANAGE)
                .build());

        return toResponse(folder);
    }

    @Transactional(readOnly = true)
    @Override
    public List<FolderResponse> getRootFolders() {
        User user = authService.getAuthenticatedUser();
        return folderRepository.findAllByOwnerAndParentIsNullAndIsDeletedFalse(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public FolderResponse getById(String id) {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, null); // null = hanya cek VIEW
        return toResponseWithChildren(folder);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FolderResponse rename(String id, FolderRequest request) throws BadRequestException {
        validationUtil.validate(request);
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateAccess(folder, user, FolderPermissionType.MANAGE);

        if (folderRepository.existsByNameAndParentAndIsDeletedFalse(request.getName(), folder.getParent())) {
            throw new BadRequestException("Folder name already exists in this location");
        }

        folder.setName(request.getName());
        folderRepository.save(folder);
        return toResponse(folder);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String id) {
        User user = authService.getAuthenticatedUser();
        Folder folder = findFolderById(id);
        validateOwner(folder, user);
        softDeleteRecursive(folder);
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

        // update jika sudah ada, insert jika belum
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

    private void softDeleteRecursive(Folder folder) {
        List<Folder> children = folderRepository.findAllByParentAndIsDeletedFalse(folder);
        for (Folder child : children) {
            softDeleteRecursive(child);
        }
        folder.setIsDeleted(true);
        folderRepository.save(folder);
    }

    private void validateOwner(Folder folder, User user) {
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only owner can delete this folder");
        }
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

    private FolderResponse toResponseWithChildren(Folder folder) {
        List<FolderResponse> children = folderRepository
                .findAllByParentAndIsDeletedFalse(folder)
                .stream()
                .map(this::toResponse)
                .toList();

        FolderResponse response = toResponse(folder);
        response.setChildren(children);
        return response;
    }
}
