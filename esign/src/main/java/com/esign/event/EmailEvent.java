package com.esign.event;

public record EmailEvent(String to, String subject, String text) {}
