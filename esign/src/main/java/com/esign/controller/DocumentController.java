package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
            @Valid @ModelAttribute DocumentRequest request) {
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
}
