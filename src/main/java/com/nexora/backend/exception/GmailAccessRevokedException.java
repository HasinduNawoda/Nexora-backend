package com.nexora.backend.exception;

public class GmailAccessRevokedException extends RuntimeException {
    public GmailAccessRevokedException(String message) {
        super(message);
    }
}
