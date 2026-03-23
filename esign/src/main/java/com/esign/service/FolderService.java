package com.esign.service;

import com.esign.constant.FolderPermissionType;
import com.esign.entities.folder.*;
import com.esign.exception.BadRequestException;
import com.esign.model.Folder;
import com.esign.model.User;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public interface FolderService {
    FolderResponse create(FolderRequest request) throws BadRequestException, AccessDeniedException;
    RootResponse getRootFolders(SearchFolderRequest request);
    SubFolderResponse getById(String id, SearchSubFolderRequest request);
    FolderResponse rename(String id, FolderRenameRequest request) throws BadRequestException;
    FolderResponse move(String id, FolderMoveRequest request) throws BadRequestException;
    FolderResponse toggleVisibility(String id);
    void addContributor(String id, FolderContributorRequest request) throws BadRequestException;
    void removeContributor(String id, String targetUserId) throws BadRequestException;
    List<FolderTrashResponse> getTrash(SearchFolderTrashRequest request);
    FolderResponse restore(String id) throws BadRequestException;
    void delete(String id);
    void validateAccess(Folder folder, User user, FolderPermissionType required);
}
