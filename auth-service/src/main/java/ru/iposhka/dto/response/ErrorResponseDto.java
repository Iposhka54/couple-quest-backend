package ru.iposhka.dto.response;

public record ErrorResponseDto(String timestamp,
                               int status,
                               String error,
                               String[] message,
                               String path) {
}