package com.esign.controller;

import com.esign.constant.ApiUrl;
import com.esign.constant.StatusMessage;
import com.esign.entities.WebResponse;
import com.esign.entities.folder.*;
import com.esign.exception.BadRequestException;
import com.esign.helper.Utilities;
import com.esign.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
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

    @Operation(
            summary = "Create folder",
            description = "<b>parentId:</b> empty = root folder, filled = sub folder. <br>" +
                    "<b>isRoleRestricted:</b> true = role only (inherit parent role), false = public. <br>" +
                    "<b>isPublic:</b> true = all eligible users can manage, false = contributor only. <br>" +
                    "<b>Note:</b> Cannot mix public and role-restricted in same hierarchy."
    )
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

    @Operation(
            summary = "Get root folders",
            description = "<b>isRole:</b> false/empty = public area, true = role area. <br>" +
                    "<b>filter:</b> mine = own folders + documents, contributor = added as contributor, empty = all. <br>" +
                    "<b>name:</b> filter by folder name."
    )
    @Parameter(name = "isRole", description = "false = public area, true = role area")
    @Parameter(name = "filter", description = "mine = own folders + documents, contributor = folders + documents where user is contributor, empty = all")
    @Parameter(name = "name", description = "Filter by folder name and document by title")
    @SecurityRequirement(name = "Authorization")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<RootResponse>> getRootFolders(
            @ParameterObject @ModelAttribute SearchFolderRequest request
    ) {
        return utilities.handleRequest(
                () -> folderService.getRootFolders(request),
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(
            summary = "Get folder by id",
            description = "<b>filter:</b> mine = own sub folders + documents, contributor = added as contributor, empty = all. <br>" +
                    "<b>name:</b> filter sub folder by name and document by title."
    )
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<SubFolderResponse>> getById(
            @PathVariable String id,
            @ParameterObject @ModelAttribute SearchSubFolderRequest request
    ) {
        return utilities.handleRequest(
                () -> folderService.getById(id, request),
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(
            summary = "Rename folder",
            description = "<b>MANAGE</b> permission required. <br>" +
                    "Folder name must be unique within the same location."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/rename", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> rename(
            @PathVariable String id,
            @RequestBody RenameRequest request
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

    @Operation(
            summary = "Move folder",
            description = "<b>Owner only.</b> <br>" +
                    "Leave 'parentId' empty to move to root. <br>" +
                    "Cannot move to itself or its own sub folder. <br>" +
                    "Cannot mix public and role-restricted folders in same hierarchy."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/move", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> move(
            @PathVariable String id,
            @RequestBody FolderMoveRequest request
    ) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return folderService.move(id, request);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }

    @Operation(
            summary = "Toggle folder visibility",
            description = "<b>Owner only.</b> <br>" +
                    "Toggle between public and private. <br>" +
                    "Public = all eligible users can manage. <br>" +
                    "Private = contributor only."
    )
    @SecurityRequirement(name = "Authorization")
    @PatchMapping(path = "/{id}/visibility", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> toggleVisibility(@PathVariable String id) {
        return utilities.handleRequest(
                () -> folderService.toggleVisibility(id),
                HttpStatus.OK,
                StatusMessage.SUCCESS_UPDATE
        );
    }

    @Operation(
            summary = "Add contributor to folder",
            description = "Add a user as contributor to a folder with specific permission (UPLOAD or MANAGE). <br><br>" +
                    "<b>Rules:</b> <br>" +
                    "- Only users with MANAGE permission can add contributors. <br>" +
                    "- Owner cannot be added as contributor (already has MANAGE permission by default). <br>" +
                    "- If user is already a contributor, their permission will be updated. <br><br>" +
                    "<b>Role-restricted folder:</b> <br>" +
                    "- Target user must have the same role as the folder's required role. <br><br>" +
                    "<b>Public folder:</b> <br>" +
                    "- Any active user can be added as contributor."
    )
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

    @Operation(
            summary = "Delete folder",
            description = "<b>MANAGE</b> permission required. <br>" +
                    "Folder and all sub folders will be soft deleted. <br>" +
                    "Can be restored from trash."
    )
    @SecurityRequirement(name = "Authorization")
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Void>> delete(@PathVariable String id) {
        return utilities.handleRequest(() -> {
            folderService.delete(id);
            return null;
        }, HttpStatus.OK, StatusMessage.SUCCESS_DELETE);
    }

    @Operation(
            summary = "Get trash folders",
            description = "<b>type:</b> public = public folders, role = role folders, empty = all. <br>" +
                    "<b>name:</b> filter by folder name. <br>" +
                    "Shows all deleted folders accessible by current user. <br>" +
                    "Only MANAGE permission can restore."
    )
    @SecurityRequirement(name = "Authorization")
    @GetMapping(path = "/trash", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<List<FolderTrashResponse>>> getTrash(
            @ParameterObject @ModelAttribute SearchFolderTrashRequest request
    ) {
        return utilities.handleRequest(
                () -> folderService.getTrash(request),
                HttpStatus.OK,
                StatusMessage.SUCCESS_RETRIEVE
        );
    }

    @Operation(
            summary = "Restore folder from trash",
            description = "<b>MANAGE</b> permission required. <br>" +
                    "All sub folders will be restored automatically. <br>" +
                    "Parent folder must be restored first if it is still in trash."
    )
    @SecurityRequirement(name = "Authorization")
    @PutMapping(path = "/{id}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<FolderResponse>> restore(@PathVariable String id) {
        return utilities.handleRequest(
                () -> {
                    try {
                        return folderService.restore(id);
                    } catch (BadRequestException e) {
                        throw new RuntimeException(e);
                    }
                },
                HttpStatus.OK,
                StatusMessage.SUCCESS_RESTORE
        );
    }
}
