package ru.iposhka.exception;

public class BadJwtException extends RuntimeException {

    public BadJwtException(String message) {
        super(message);
    }
}
