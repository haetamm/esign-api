package com.esign.service;

import com.esign.entities.folder.*;
import com.esign.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public interface FolderService {
    FolderResponse create(FolderRequest request) throws BadRequestException, AccessDeniedException;
    List<FolderResponse> getRootFolders(SearchFolderRequest request);
    FolderResponse getById(String id, SearchSubFolderRequest request);
    FolderResponse rename(String id, FolderRenameRequest request) throws BadRequestException;
    FolderResponse move(String id, FolderMoveRequest request) throws BadRequestException;
    FolderResponse toggleVisibility(String id);
    void addContributor(String id, FolderContributorRequest request) throws BadRequestException;
    void removeContributor(String id, String targetUserId) throws BadRequestException;
    List<FolderTrashResponse> getTrash(SearchFolderTrashRequest request);
    FolderResponse restore(String id) throws BadRequestException;
    void delete(String id);
}
