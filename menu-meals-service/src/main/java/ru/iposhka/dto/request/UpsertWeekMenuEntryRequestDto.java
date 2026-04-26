package ru.iposhka.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertWeekMenuEntryRequestDto(
        @NotNull(message = "coupleId обязателен")
        Long coupleId,
        @Valid
        @NotNull(message = "Данные записи обязательны")
        WeekMenuEntryRequestDto entry
) {}