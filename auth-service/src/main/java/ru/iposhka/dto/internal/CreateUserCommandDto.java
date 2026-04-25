package ru.iposhka.dto.internal;

import ru.iposhka.model.Gender;

public record CreateUserCommandDto(
        String email,
        String password,
        String name,
        Gender gender,
        Boolean emailVerified
) {
}