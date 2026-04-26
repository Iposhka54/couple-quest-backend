package ru.iposhka.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MealIngredientRequestDto(
        @NotBlank(message = "Название ингредиента обязательно")
        String name,
        @NotNull(message = "Количество ингредиента обязательно")
        @DecimalMin(value = "0.001", message = "Количество ингредиента должно быть больше 0")
        BigDecimal amount,
        @NotBlank(message = "Единица измерения ингредиента обязательна")
        String unit
) {}