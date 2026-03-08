package com.esign.helper;

import com.esign.entities.PaginationResponse;
import com.esign.entities.WebErrorResponse;
import com.esign.entities.WebResponse;
import com.esign.entities.role.SearchRoleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
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
public class Utilities {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

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
}
