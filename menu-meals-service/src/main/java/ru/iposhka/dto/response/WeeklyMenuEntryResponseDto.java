package ru.iposhka.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import ru.iposhka.model.MealType;

public record WeeklyMenuEntryResponseDto(
        Long id,
        LocalDate plannedDate,
        MealType mealType,
        Integer servings,
        String note,
        Long updatedByUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        MealResponseDto meal
) {}