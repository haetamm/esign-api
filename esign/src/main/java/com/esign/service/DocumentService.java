package com.esign.service;

import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
import com.esign.entities.folder.RenameRequest;
import com.esign.exception.BadRequestException;
import com.esign.exception.InternalServerException;
import com.esign.exception.NotFoundException;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.MalformedURLException;


public interface DocumentService {
    DocumentResponse upload(DocumentRequest request) throws InternalServerException, BadRequestException, IOException;
    Resource getDocumentById(String folder_id, String document_id) throws NotFoundException, MalformedURLException;
    DocumentResponse rename(String id, RenameRequest request);
}
