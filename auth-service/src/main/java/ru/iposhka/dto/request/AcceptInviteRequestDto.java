package ru.iposhka.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequestDto(
        @NotBlank(message = "Токен приглашения обязателен")
        String token
) {}