package com.esign.service;

import com.esign.entities.folder.FolderContributorRequest;
import com.esign.entities.folder.FolderRequest;
import com.esign.entities.folder.FolderResponse;
import com.esign.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

public interface FolderService {
    FolderResponse create(FolderRequest request) throws BadRequestException, AccessDeniedException;
    List<FolderResponse> getRootFolders();
    FolderResponse getById(String id);
    FolderResponse rename(String id, FolderRequest request) throws BadRequestException;
    void delete(String id);
    void addContributor(String id, FolderContributorRequest request) throws BadRequestException;
    void removeContributor(String id, String targetUserId) throws BadRequestException;
}
