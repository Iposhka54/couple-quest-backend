package ru.iposhka.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplaceWeekMenuRequestDto(
        @NotNull(message = "coupleId обязателен")
        Long coupleId,
        @Valid
        List<WeekMenuEntryRequestDto> entries
) {}