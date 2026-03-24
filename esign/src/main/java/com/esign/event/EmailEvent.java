package com.esign.event;

import java.io.Serializable;

public record EmailEvent(String to, String subject, String text) implements Serializable {}
