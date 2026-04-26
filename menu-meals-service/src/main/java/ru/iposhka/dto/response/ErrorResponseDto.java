package ru.iposhka.dto.response;

public record ErrorResponseDto(
        String timestamp,
        String[] message,
        String path
) {}