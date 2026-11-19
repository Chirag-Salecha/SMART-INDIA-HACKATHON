package com.sih.bidcompliance.exception;

/**
 * Thrown when an uploaded file fails validation (empty, wrong type, too
 * large). Handled by GlobalExceptionHandler and translated into a 400.
 */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
