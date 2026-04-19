package ru.iposhka.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailCodeRequestDto(
        @Email(message = "Некорректный формат email")
        @NotBlank(message = "Email обязателен")
        String email
) {
}