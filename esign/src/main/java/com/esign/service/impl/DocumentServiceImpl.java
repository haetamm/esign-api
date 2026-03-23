package com.esign.service.impl;

import com.esign.constant.ContributorStatus;
import com.esign.constant.DocumentStatus;
import com.esign.constant.FolderPermissionType;
import com.esign.constant.StatusMessage;
import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final ValidationUtil validationUtil;
    private final Utilities utilities;
    private final AuthService authService;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final DocumentContributorRepository documentContributorRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FolderService folderService;
    private final UserRoleRepository userRoleRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse upload(DocumentRequest request) throws IOException, BadRequestException {
        validationUtil.validate(request);
        User owner = authService.getAuthenticatedUser();

        Role requiredRole = null;
        if ((request.getFolderId() == null || request.getFolderId().isBlank()) || request.getIsRoleRestricted()) {
            requiredRole = userRoleRepository.findByUser(owner)
                    .stream()
                    .findFirst()
                    .map(UserRole::getRole)
                    .orElseThrow(() -> new AccessDeniedException("You don't have the required role"));
        }

        Folder folder = null;
        if (request.getFolderId() != null && !request.getFolderId().isBlank()) {
            folder = folderRepository.findByIdAndIsDeletedFalse(request.getFolderId())
                    .orElseThrow(() -> new NotFoundException("Folder not found"));

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
            // validasi owner untuk root document
//            if (!document.getOwner().getId().equals(owner.getId())) {
//                throw new AccessDeniedException("You are not the owner of this document");
//            }
        } else {
            // document dalam folder — validasi folder dan akses
            Folder folder = folderRepository.findByIdAndIsDeletedFalse(folderId)
                    .orElseThrow(() -> new NotFoundException("Folder not found"));

            if (!folder.getIsPublic()) {
                folderService.validateAccess(folder, owner, FolderPermissionType.UPLOAD);
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

    private Document findById(String id) throws NotFoundException {
        return documentRepository.findById(id).orElseThrow(() -> new NotFoundException(StatusMessage.DOCUMENT_NOT_FOUND));
    }

    private void addContributors(Document document, List<String> contributorIds) {
        contributorIds.forEach(contributorId -> {
            User contributor = userRepository.findByIdAndIsEnableTrue(contributorId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + contributorId));

            if (document.getRequiredRole() != null) {
                boolean hasRole = userRoleRepository.findByUser(contributor)
                        .stream()
                        .anyMatch(ur -> ur.getRole().getId()
                                .equals(document.getRequiredRole().getId()));
                if (!hasRole) {
                    throw new AccessDeniedException("User " + contributor.getUsername() + " does not have the required role");
                }
            }

            if (documentContributorRepository.existsByDocumentAndUser(document, contributor)) {
                try {
                    throw new BadRequestException("User " + contributor.getUsername() + " is already a contributor");
                } catch (BadRequestException e) {
                    throw new RuntimeException(e);
                }
            }

            DocumentContributor documentContributor = documentContributorRepository.save(
                    DocumentContributor.builder()
                            .document(document)
                            .user(contributor)
                            .status(ContributorStatus.PENDING)
                            .build()
            );


            String subject = "Document Signature Request - " + documentContributor.getDocument().getTitle();
            String text = String.format("Halo " + documentContributor.getUser().getProfile().getName() + ",\n\n" +
                    "Anda ditambahkan sebagai penandatangan document: " + documentContributor.getDocument().getTitle() + "\n" +
                    "Silakan login untuk menandatangani document tersebut.\n\n" +
                    "Terima kasih.");

            emailService.sendEmailAfterCommit(documentContributor.getUser().getEmail(), subject, text);

        });
    }

}
