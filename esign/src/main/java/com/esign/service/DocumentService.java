package com.esign.service;

import com.esign.entities.document.DocumentRequest;
import com.esign.entities.document.DocumentResponse;
import com.esign.exception.BadRequestException;
import com.esign.exception.InternalServerException;

import java.io.IOException;


public interface DocumentService {
    // interface
    DocumentResponse upload(DocumentRequest request) throws InternalServerException, BadRequestException, IOException;
}
