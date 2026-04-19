package ru.iposhka.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailCodeRequestDto(
        @Email(message = "Некорректный формат email")
        @NotBlank(message = "Email обязателен")
        String email,
        @NotBlank(message = "Код обязателен")
        @Pattern(regexp = "\\d{6}", message = "Код должен содержать 6 цифр")
        String code
) {
}