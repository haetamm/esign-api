package com.esign.service.impl;

import com.esign.constant.ContributorStatus;
import com.esign.constant.DocumentStatus;
import com.esign.constant.FolderPermissionType;
import com.esign.entities.document.DocumentContributorResponse;
import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
import com.esign.exception.BadRequestException;
import com.esign.exception.InternalServerException;
import com.esign.exception.NotFoundException;
import com.esign.model.*;
import com.esign.repository.*;
import com.esign.service.*;
import com.esign.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {
    private final ValidationUtil validationUtil;
    private final AuthService authService;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final UserRoleRepository userRoleRepository;
    private final FolderContributorRepository folderContributorRepository;
    private final DocumentContributorRepository documentContributorRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FolderService folderService;

    // impl
    @Transactional(rollbackFor = Exception.class)
    @Override
    public DocumentResponse upload(DocumentRequest request) throws IOException {
        validationUtil.validate(request);
        User owner = authService.getAuthenticatedUser();

        // 2. validasi folder
        Folder folder = null;
        if (request.getFolderId() != null) {
            folder = folderRepository.findByIdAndIsDeletedFalse(request.getFolderId())
                    .orElseThrow(() -> new NotFoundException("Folder not found"));
            folderService.validateAccess(folder, owner, FolderPermissionType.UPLOAD);
        }

        // 3. simpan file ke storage
        String documentId = UUID.randomUUID().toString();
        String category = storageService.resolveCategory(folder);
        String fileName = StringUtils.cleanPath(String.valueOf(Objects.requireNonNull(request.getDocument())));


        Path documentDir = storageService.getDocumentPath(category, documentId);
        Path filePath = documentDir.resolve("original.pdf");
        Files.copy(request.getDocument().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. simpan document ke DB
        Document document = documentRepository.save(Document.builder()
                .id(documentId)
                .title(request.getTitle())
                .filePath(filePath.toString())
                .fileName(fileName)
                .fileSize(request.getDocument().getSize())
                .owner(owner)
                .folder(folder)
                .deadline(LocalDateTime.parse(request.getDeadline()))
                .status(DocumentStatus.DRAFT)
                .build());

        // 5. set contributor jika ada
        if (request.getContributorIds() != null && !request.getContributorIds().isEmpty()) {
            addContributors(document, request.getContributorIds());
            document.setStatus(DocumentStatus.WAITING_SIGNATURE);
            documentRepository.save(document);
        }

        return toResponse(document);


    }

    private void addContributors(Document document, List<String> contributorIds) {
        contributorIds.forEach(contributorId -> {
            User contributor = userRepository.findByIdAndIsEnableTrue(contributorId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + contributorId));

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
            String text =    "Halo " + documentContributor.getUser().getUsername() + ",\n\n" +
                    "Anda ditambahkan sebagai penandatangan document: " + documentContributor.getDocument().getTitle() + "\n" +
                    "Silakan login untuk menandatangani document tersebut.\n\n" +
                    "Terima kasih.";

            emailService.sendEmail(documentContributor.getUser().getEmail(), subject, text);

        });
    }

    private DocumentResponse toResponse(Document document) {
        List<DocumentContributorResponse> contributors = documentContributorRepository
                .findAllByDocument(document)
                .stream()
                .map(c -> DocumentContributorResponse.builder()
                        .id(c.getId())
                        .userId(c.getUser().getId())
                        .username(c.getUser().getUsername())
                        .status(c.getStatus())
                        .signedAt(c.getSignedAt() != null ? c.getSignedAt().toString() : null)
                        .reason(c.getReason())
                        .build())
                .toList();

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .folderName(document.getFolder() != null ? document.getFolder().getName() : null)
                .ownerUsername(document.getOwner().getUsername())
                .contributors(contributors)
                .deadline(document.getDeadline() != null ? document.getDeadline().toString() : null)
                .createdAt(document.getCreatedAt().toString())
                .updatedAt(document.getUpdatedAt().toString())
                .build();
    }
}
