package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.folder.FolderContributorRequest;
import com.esign.entities.folder.FolderRequest;
import com.esign.entities.folder.FolderResponse;
import com.esign.exception.BadRequestException;
import com.esign.helper.Utilities;
import com.esign.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiUrl.API_URL + ApiUrl.API_FOLDER)
@RequiredArgsConstructor
@Tag(name = "Folder", description = "Folder Management API")
public class FolderController {

    private final FolderService folderService;
    private final Utilities utilities;

    @Operation(summary = "Create folder")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> create(@RequestBody FolderRequest request) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return folderService.create(request);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.CREATED,
                StatusMessage.SUCCESS_CREATE
        );
    }

    @Operation(summary = "Get root folders")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<FolderResponse>>> getRootFolders() {
        return utilities.handleRequest(
                folderService::getRootFolders,
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Get folder by id")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> getById(@PathVariable String id) {
        return utilities.handleRequest(
                () -> folderService.getById(id),
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(summary = "Rename folder")
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> rename(
            @PathVariable String id,
            @RequestBody FolderRequest request
    ) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return folderService.rename(id, request);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }

    @Operation(summary = "Delete folder")
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> delete(@PathVariable String id) {
        return utilities.handleRequest(() -> {
            folderService.delete(id);
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_DELETE);
    }

    @Operation(summary = "Add contributor to folder")
    @SecurityRequirement(name = "Authorization")
    @PostMapping(path = "/{id}/contributors", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> addContributor(
            @PathVariable String id,
            @RequestBody FolderContributorRequest request
    ) {
        return utilities.handleRequest(() -> {
            try {
                folderService.addContributor(id, request);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_CREATE);
    }

    @Operation(summary = "Remove contributor from folder")
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping(path = "/{id}/contributors/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> removeContributor(
            @PathVariable String id,
            @PathVariable String userId
    ) {
        return utilities.handleRequest(() -> {
            try {
                folderService.removeContributor(id, userId);
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_DELETE);
    }
}
