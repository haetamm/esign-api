package com.esign.service;

public interface EmailService {
    void sendEmail(String email, String subject, String text);
}
