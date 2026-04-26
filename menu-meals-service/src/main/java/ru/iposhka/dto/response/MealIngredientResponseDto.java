package ru.iposhka.dto.response;

import java.math.BigDecimal;

public record MealIngredientResponseDto(
        Long id,
        String name,
        BigDecimal amount,
        String unit
) {}