package com.sih.bidcompliance.exception;

/**
 * Thrown when a requested entity (tender, bidder, document, ...) does not
 * exist. Handled by GlobalExceptionHandler and translated into a 404.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
