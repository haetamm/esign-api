package com.esign.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ValidationCustomException extends Exception {
    private final String path;

    public ValidationCustomException(String message, String path) {
        super(message);
        this.path = path;
    }
}