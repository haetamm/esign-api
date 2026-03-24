package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.document.ContributorRequest;
import com.esign.entities.document.DocumentMoveRequest;
import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
import com.esign.entities.folder.RenameRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.InternalServerException;
import com.esign.helper.Utilities;
import com.esign.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_DOCUMENT)
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document Management API")
public class DocumentController {

    private final DocumentService documentService;
    private final Utilities utilities;

    @Operation(
            summary = "Upload document",
            description = "<b>document:</b> PDF only. <br>" +
                    "<b>folderId:</b> empty = root (public storage). <br>" +
                    "<b>isRoleRestricted:</b> true = document restricted to your role. <br>" +
                    "<b>contributorIds:</b> empty = DRAFT, filled = WAITING_SIGNATURE. <br>" +
                    "<b>deadline:</b> format yyyy-MM-dd."
    )
    @SecurityRequirement(name = "Authorization")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<DocumentResponse>> upload(
            @Valid @ModelAttribute DocumentRequest request
    ) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return documentService.upload(request);
                    } catch (InternalServerException | BadRequestException | IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.CREATED,
                StatusMessage.SUCCESS_CREATE
        );
    }

    @Operation(
            summary = "Get document by id",
            description = "Public folder: accessible by anyone. <br>" +
                    "Private folder: accessible by folder owner or any contributor (UPLOAD/MANAGE). <br>" +
                    "<b>folder_id:</b> optional, only for documents inside a folder."
    )
    @SecurityRequirement(name = "Authorization")
    @GetMapping("/{document_id}")
    public ResponseEntity<Resource> getDocumentById(
            @PathVariable String document_id,
            @RequestParam(required = false) String folder_id
    ) throws IOException {
        Resource document = documentService.getDocumentById(folder_id, document_id);
        Path filePath = document.getFile().toPath();
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(document);
    }

    @Operation(
            summary = "Rename document",
            description = "Public folder: accessible by anyone. <br>" +
                    "Private folder: accessible by folder owner or any contributor (UPLOAD/MANAGE). <br>" +
                    "Cannot rename if document role does not match your role."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/rename", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> rename(
            @PathVariable String id,
            @RequestBody RenameRequest request
    ) {
        return utilities.handleRequest(
                () -> documentService.rename(id, request),
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }

    @Operation(
            summary = "Move document",
            description = "<b>Owner only.</b> <br>" +
                    "Only allowed in <b>DRAFT</b> status. <br>" +
                    "Cannot move role-restricted document to public folder or vice versa. <br>" +
                    "Cannot move to folder with different role. <br>" +
                    "<b>folderId:</b> empty = move to root."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/move", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> move(
            @PathVariable String id,
            @RequestBody DocumentMoveRequest request
    ) {
        return utilities.handleRequest(() -> {
            try {
                return documentService.move(id, request);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(
            summary = "Add contributor",
            description = "<b>Owner only.</b> <br>" +
                    "Only allowed in <b>DRAFT</b> or <b>WAITING_SIGNATURE</b> status. <br>" +
                    "If document is role-restricted, contributors must have the same role. <br>" +
                    "Status will change to <b>WAITING_SIGNATURE</b> if previously DRAFT."
    )
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/{id}/contributors", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> addContributor(
            @PathVariable String id,
            @RequestBody ContributorRequest request
            ) {
        return utilities.handleRequest(() -> {
            try {
                return documentService.addContributor(id, request);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_UPDATE);
    }

    @Operation(
            summary = "Remove contributor",
            description = "<b>Owner only.</b> <br>" +
                    "Only allowed in <b>DRAFT</b> or <b>WAITING_SIGNATURE</b> status. <br>" +
                    "If all contributors removed, status will revert to <b>DRAFT</b>. <br>" +
                    "Removed contributor will be notified via email."
    )
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping(path = "/{id}/contributors/{contributorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> removeContributor(@PathVariable String id, @PathVariable String contributorId) {
        return utilities.handleRequest(() -> {
            try {
                return documentService.removeContributor(id, contributorId);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_DELETE);
    }

    @Operation(
            summary = "Delete document",
            description = "<b>Owner</b> can delete any document. <br>" +
                    "Non-owner requires <b>MANAGE</b> permission on the folder. <br>" +
                    "Cannot delete document with status <b>IN_PROGRESS</b>. <br>" +
                    "Document will be moved to trash (soft delete)."
    )
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> delete(@PathVariable String id) {
        return utilities.handleRequest(() -> {
            try {
                documentService.delete(id);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_DELETE);
    }

    @Operation(
            summary = "Restore document",
            description = "<b>Owner</b> can restore any document. <br>" +
                    "Non-owner requires <b>MANAGE</b> permission on the folder. <br>" +
                    "Original folder must exist and not be in trash. <br>" +
                    "If original folder is in trash, restore the folder first."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> restore(@PathVariable String id) {
        return utilities.handleRequest(() -> {
            try {
                return documentService.restore(id);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, HttpStatus.OK, StatusMessage.SUCCESS_RESTORE);
    }

}
