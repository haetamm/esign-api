package com.esign.service;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendEmailAfterCommit(String to, String subject, String text);
}
