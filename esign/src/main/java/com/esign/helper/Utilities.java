package com.esign.helper;

import com.esign.entities.PaginationResponse;
import com.esign.entities.WebErrorResponse;
import com.esign.entities.WebResponse;
import com.esign.entities.document.DocumentContributorResponse;
import com.esign.entities.document.DocumentResponse;
import com.esign.model.Document;
import com.esign.repository.DocumentContributorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class Utilities {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentContributorRepository documentContributorRepository;

    public <T> ResponseEntity<WebResponse<T>> handleRequest(Supplier<T> requestHandler, HttpStatus status, String message, PaginationResponse paginationResponse) {
        T data = requestHandler.get();
        WebResponse<T> response = new WebResponse<>(
                status.value(),
                message,
                data,
                paginationResponse
        );
        return ResponseEntity.status(status).body(response);
    }

    public <T> ResponseEntity<WebResponse<T>> handleRequest(Supplier<T> requestHandler, HttpStatus status, String message) {
        return handleRequest(requestHandler, status, message, null);
    }

    public void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        WebErrorResponse<String> errorResponse = new WebErrorResponse<>();
        errorResponse.setCode(status.value());
        errorResponse.setStatus(status.name());
        errorResponse.setMessages(message);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    public Pageable buildPageable(Integer page, Integer size, String sortBy, String direction, String defaultSortBy) {
        String resolvedDirection = (direction != null && (direction.equalsIgnoreCase("ASC") || direction.equalsIgnoreCase("DESC")))
                ? direction : "ASC";
        int resolvedPage = page != null && page > 0 ? page : 1;
        int resolvedSize = size != null ? size : 10;
        String resolvedSortBy = sortBy != null && !sortBy.isBlank() ? sortBy : defaultSortBy;

        Sort sort = Sort.by(Sort.Direction.fromString(resolvedDirection), resolvedSortBy);
        return PageRequest.of(resolvedPage - 1, resolvedSize, sort);
    }

    public DocumentResponse documentResponse(Document document) {
        List<DocumentContributorResponse> contributors = documentContributorRepository
                .findAllByDocument(document)
                .stream()
                .map(c -> DocumentContributorResponse.builder()
                        .id(c.getId())
                        .userId(c.getUser().getId())
                        .username(c.getUser().getUsername())
                        .status(c.getStatus())
                        .signedAt(c.getSignedAt() != null ? c.getSignedAt().toString() : null)
                        .reason(c.getReason())
                        .build())
                .toList();

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileSize(document.getFileSize())
                .status(document.getStatus())
                .folderName(document.getFolder() != null ? document.getFolder().getName() : null)
                .ownerUsername(document.getOwner().getUsername())
                .contributors(contributors)
                .deadline(document.getDeadline() != null ? document.getDeadline().toString() : null)
                .createdAt(document.getCreatedAt().toString())
                .updatedAt(document.getUpdatedAt().toString())
                .build();
    }
}
