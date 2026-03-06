package com.esign.helper;

import com.esign.entities.PaginationResponse;
import com.esign.entities.WebResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import java.security.SecureRandom;
import java.util.function.Supplier;

@Component
public class Utilities {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

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

}
