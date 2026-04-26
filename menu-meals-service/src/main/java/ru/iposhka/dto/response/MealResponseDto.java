package ru.iposhka.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MealResponseDto(
        Long id,
        Long coupleId,
        Long createdByUserId,
        String name,
        String description,
        String portionSize,
        BigDecimal calories,
        BigDecimal proteins,
        BigDecimal fats,
        BigDecimal carbs,
        List<MealIngredientResponseDto> ingredients,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}