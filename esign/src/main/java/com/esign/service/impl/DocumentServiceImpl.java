package com.esign.service.impl;

import com.esign.constant.ContributorStatus;
import com.esign.constant.DocumentStatus;
import com.esign.constant.FolderPermissionType;
import com.esign.constant.StatusMessage;
import com.esign.entities.document.ContributorRequest;
import com.esign.entities.document.DocumentMoveRequest;
import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
import com.esign.entities.folder.RenameRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.NotFoundException;
import com.esign.helper.Utilities;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.*;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final ValidationUtil validationUtil;
    private final Utilities utilities;
    private final AuthService authService;
    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final DocumentContributorRepository documentContributorRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FolderService folderService;
    private final UserRoleRepository userRoleRepository;
    private final FolderRepository folderRepository;
    private final FolderContributorRepository folderContributorRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse upload(DocumentRequest request) throws IOException, BadRequestException {
        validationUtil.validate(request);
        User owner = authService.getAuthenticatedUser();

        Role requiredRole = null;
        if (request.getIsRoleRestricted()) {
            requiredRole = userRoleRepository.findByUser(owner)
                    .stream()
                    .findFirst()
                    .map(UserRole::getRole)
                    .orElseThrow(() -> new AccessDeniedException("You don't have the required role to access this resource"));
        }

        Folder folder = null;
        if (request.getFolderId() != null && !request.getFolderId().isBlank()) {
            folder = folderService.getEntityById(request.getFolderId());

            if (!folder.getIsPublic()) {
                folderService.validateAccess(folder, owner, FolderPermissionType.UPLOAD);
            }
        }

        String documentId = UUID.randomUUID().toString();
        String category = storageService.resolveCategory(folder);
        String fileName = request.getTitle();

        Path documentDir = storageService.getDocumentPath(category, documentId);
        Path filePath = documentDir.resolve("original.pdf");
        Files.copy(request.getDocument().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        try {
            Document document = documentRepository.save(Document.builder()
                    .id(documentId)
                    .title(request.getTitle())
                    .filePath(filePath.toString())
                    .fileName(fileName)
                    .fileSize(request.getDocument().getSize())
                    .owner(owner)
                    .folder(folder)
                    .requiredRole(requiredRole)
                    .deadline(LocalDate.parse(request.getDeadline()).atTime(23, 59, 59))
                    .status(DocumentStatus.DRAFT)
                    .build());

            if (request.getContributorIds() != null && !request.getContributorIds().isEmpty()) {
                addContributors(document, request.getContributorIds());
                document.setStatus(DocumentStatus.WAITING_SIGNATURE);
                documentRepository.save(document);
            }

            return utilities.documentResponse(document);

        } catch (Exception e) {
            // cleanup folder + file jika DB gagal
            storageService.deleteDocumentFiles(category, documentId);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Resource getDocumentById(String folderId, String documentId) throws MalformedURLException {
        User owner = authService.getAuthenticatedUser();
        Document document = findById(documentId);

        if (folderId == null) {
            if (document.getFolder() != null) {
                throw new AccessDeniedException("Document is inside a folder, provide folder_id");
            }
        } else {
            Folder folder = folderService.getEntityById(folderId);

            if (!folder.getIsPublic()) {
                boolean isOwner = folder.getOwner().getId().equals(owner.getId());
                boolean isContributor = folderContributorRepository
                        .existsByFolderAndUser(folder, owner);

                if (!isOwner && !isContributor) {
                    throw new AccessDeniedException("You don't have access to this resources");
                }
            }

            if (document.getFolder() == null || !folderId.equals(document.getFolder().getId())) {
                throw new NotFoundException("Document does not belong to this folder");
            }
        }

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new NotFoundException("File not found or not readable: " + document.getFilePath());
        }

        return new UrlResource(filePath.toUri());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse rename(String id, RenameRequest request) {
        validationUtil.validate(request);
        User user = authService.getAuthenticatedUser();
        Document document = findById(id);

        validateUserMatchesDocumentRole(document, user);

        Folder folder = null;
        if (document.getFolder() != null) {
            folder = folderService.getEntityById(document.getFolder().getId());

            if (!folder.getIsPublic()) {
                boolean isOwner = folder.getOwner().getId().equals(user.getId());
                boolean isContributor = folderContributorRepository
                        .existsByFolderAndUser(folder, user);

                if (!isOwner && !isContributor) {
                    throw new AccessDeniedException("You don't have access to this resources");
                }
            }
        }

        document.setTitle(request.getName());
        document.setFileName(request.getName());
        return utilities.documentResponse(documentRepository.save(document));

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse move(String id, DocumentMoveRequest request) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Document document = findById(id);

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new BadRequestException("Cannot move document with status " + document.getStatus());
        }

        validateOwner(document, user);
        validateUserMatchesDocumentRole(document, user);

        Folder newFolder = null;
        if (request.getFolderId() != null && !request.getFolderId().isBlank()) {
            newFolder = folderService.getEntityById(request.getFolderId());

            boolean documentIsRole = document.getRequiredRole() != null;
            boolean newFolderIsRole = newFolder.getRequiredRole() != null;

            if (documentIsRole && !newFolderIsRole) {
                throw new BadRequestException("Cannot move role-restricted document to public folder");
            }
            if (!documentIsRole && newFolderIsRole) {
                throw new BadRequestException("Cannot move public document to role-restricted folder");
            }

            // jika role restricted, cek role sama
            if (documentIsRole) {
                if (!document.getRequiredRole().getId().equals(newFolder.getRequiredRole().getId())) {
                    throw new BadRequestException("Cannot move document to folder with different role");
                }
            }

            if (!newFolder.getIsPublic()) {
                folderService.validateAccess(newFolder, user, FolderPermissionType.UPLOAD);
            }
        }

        document.setFolder(newFolder);
        return utilities.documentResponse(documentRepository.save(document));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse addContributor(String id, ContributorRequest request) throws BadRequestException {
        validationUtil.validate(request);
        User user = authService.getAuthenticatedUser();
        Document document = findById(id);

        validateOwner(document, user);
        validateEditable(document);
        addContributors(document, request.getContributorIds());

        if (document.getStatus() == DocumentStatus.DRAFT) {
            document.setStatus(DocumentStatus.WAITING_SIGNATURE);
            documentRepository.save(document);
        }

        return utilities.documentResponse(documentRepository.save(document));

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse removeContributor(String id, String contributorId) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Document document = findById(id);

        validateEditable(document);
        validateOwner(document, user);
        validateUserMatchesDocumentRole(document, user);

        DocumentContributor contributor = documentContributorRepository.findById(contributorId)
                .orElseThrow(() -> new NotFoundException("Contributor not found"));

        // pastikan contributor milik document ini
        if (!contributor.getDocument().getId().equals(document.getId())) {
            throw new BadRequestException("Contributor does not belong to this document");
        }

        documentContributorRepository.delete(contributor);

        // jika semua contributor dihapus → kembali ke DRAFT
        if (documentContributorRepository.findAllByDocument(document).isEmpty()) {
            document.setStatus(DocumentStatus.DRAFT);
            documentRepository.save(document);
        }

        String subject = "Document Signature Request Cancelled - " + document.getTitle();
        String text = "Hello " + contributor.getUser().getProfile().getName() + ",\n\n" +
                "You have been removed as a signer from the document: " + document.getTitle() + "\n\n" +
                "Thank you.";
        emailService.sendEmailAfterCommit(contributor.getUser().getEmail(), subject, text);

        return utilities.documentResponse(document);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String id) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Document document = findById(id);

        // cek akses, hanya owner/uploader atau user yg memiliki MANAGE folder permission
        if (!document.getOwner().getId().equals(user.getId())) {
            if (document.getFolder() != null) {
                folderService.validateAccess(document.getFolder(), user, FolderPermissionType.MANAGE);
            } else {
                throw new AccessDeniedException("Only uploader can delete this document");
            }
        }

        // tidak bisa delete jika IN_PROGRESS
        if (document.getStatus() == DocumentStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot delete document with status IN_PROGRESS");
        }

        document.setIsDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(user);
        document.setOriginalFolderId(
                document.getFolder() != null ? document.getFolder().getId() : null
        );
        documentRepository.save(document);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse restore(String id) throws BadRequestException {
        User user = authService.getAuthenticatedUser();
        Document document = documentRepository.findByIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new NotFoundException("Document not found in trash"));

        // cek akses, hanya owner atau user yg memiliki MANAGE folder permission
        if (!document.getOwner().getId().equals(user.getId())) {
            if (document.getFolder() != null) {
                folderService.validateAccess(document.getFolder(), user, FolderPermissionType.MANAGE);
            } else {
                throw new AccessDeniedException("Only uploader can restore this document");
            }
        }

        // cek folder asal masih ada
        if (document.getOriginalFolderId() != null) {
            Folder originalFolder = folderRepository.findById(document.getOriginalFolderId())
                    .orElseThrow(() -> new BadRequestException("Cannot restore, original folder has been permanently deleted"));

            if (originalFolder.getIsDeleted()) {
                throw new BadRequestException("Cannot restore, original folder '" + originalFolder.getName() + "' is still in trash. Restore it first");
            }

            document.setFolder(originalFolder);
        }

        document.setIsDeleted(false);
        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setOriginalFolderId(null);

        return utilities.documentResponse(documentRepository.save(document));

    }

    private Document findById(String id) throws NotFoundException {
        return documentRepository.findById(id).orElseThrow(() -> new NotFoundException(StatusMessage.DOCUMENT_NOT_FOUND));
    }

    private void addContributors(Document document, List<String> contributorIds) throws BadRequestException {
        // fetch dan cek semua contributor/signer sekaligus
        List<User> contributors = userRepository.findAllByIdInAndIsEnableTrue(contributorIds);
        if (contributors.size() != contributorIds.size()) {
            throw new NotFoundException("One or more users not found or inactive");
        }

        // validasi role sekaligus
        if (document.getRequiredRole() != null) {
            List<String> userIds = contributors.stream().map(User::getId).toList();
            List<String> validUserIds = userRoleRepository
                    .findAllByUserIdInAndRoleId(userIds, document.getRequiredRole().getId())
                    .stream()
                    .map(ur -> ur.getUser().getId())
                    .toList();

            List<String> invalidUsers = contributors.stream()
                    .filter(u -> !validUserIds.contains(u.getId()))
                    .map(User::getUsername)
                    .toList();

            if (!invalidUsers.isEmpty()) {
                throw new AccessDeniedException(
                        "Users do not have the required role: " + String.join(", ", invalidUsers)
                );
            }
        }

        // cek duplikat sekaligus
        List<User> existingContributors = documentContributorRepository
                .findUsersByDocument(document);

        List<String> duplicates = contributors.stream()
                .filter(existingContributors::contains)
                .map(User::getUsername)
                .toList();

        if (!duplicates.isEmpty()) {
            throw new BadRequestException(
                    "Users already a signer: " + String.join(", ", duplicates)
            );
        }

        // batch insert semua contributor/signer sekaligus
        List<DocumentContributor> newContributors = contributors.stream()
                .map(contributor -> DocumentContributor.builder()
                        .document(document)
                        .user(contributor)
                        .status(ContributorStatus.PENDING)
                        .build())
                .toList();

        documentContributorRepository.saveAll(newContributors);

        // publish semua email event sekaligus
        newContributors.forEach(dc ->
                emailService.sendEmailAfterCommit(
                        dc.getUser().getEmail(),
                        "Document Signature Request - " + document.getTitle(),
                        String.format("Hello " + dc.getUser().getProfile().getName() + ",\n\n" +
                                "You have been added as a signer for the document: " + document.getTitle() + "\n" +
                                "Please log in to sign the document.\n\n" +
                                "Thank you.")
                )
        );
    }

    private void validateOwner(Document document, User user) {
        if (!document.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the uploader can access this resource.");
        }
    }

    private void validateEditable(Document document) throws BadRequestException {
        if (document.getStatus() == DocumentStatus.IN_PROGRESS ||
                document.getStatus() == DocumentStatus.COMPLETED ||
                document.getStatus() == DocumentStatus.REJECTED) {
            throw new BadRequestException("Document cannot be modified when status is " + document.getStatus());
        }
    }

    private void validateUserMatchesDocumentRole(Document document, User user) {
        if (document.getRequiredRole() != null) {
            boolean hasRole = userRoleRepository.findByUser(user)
                    .stream()
                    .anyMatch(ur -> ur.getRole().getId()
                            .equals(document.getRequiredRole().getId()));

            if (!hasRole) {
                throw new AccessDeniedException(
                        "You don't have the required role '" + document.getRequiredRole().getName() + "' to access this resource"
                );
            }
        }
    }
}
