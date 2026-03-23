package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
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
            description = "<b>document:</b> PDF only, max 10MB. <br>" +
                    "<b>folderId:</b> empty = root (public storage). <br>" +
                    "<b>contributorIds:</b> empty = DRAFT, filled = WAITING_SIGNATURE. <br>" +
                    "<b>deadline:</b> optional, format yyyy-MM-dd HH:mm:ss."
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
            description = "Retrieve a document file by its ID. If the document is inside a folder, provide folder_id as query parameter. Leave folder_id empty for root documents."
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
            description = "Folder <b>MANAGE</b> permission required. <br>"
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/rename", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<DocumentResponse>> rename(
            @PathVariable String id,
            @RequestBody RenameRequest request
    ) {
        return utilities.handleRequest(
                () -> {
                    return documentService.rename(id, request);
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }
}
