package ru.iposhka.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record MealUpsertRequestDto(
        @NotNull(message = "coupleId обязателен")
        Long coupleId,
        @NotBlank(message = "Название блюда обязательно")
        String name,
        String description,
        @NotBlank(message = "Размер порции обязателен")
        String portionSize,
        @NotNull(message = "Калории обязательны")
        @DecimalMin(value = "0.0", inclusive = false, message = "Калории должны быть больше 0")
        BigDecimal calories,
        @NotNull(message = "Белки обязательны")
        @DecimalMin(value = "0.0", inclusive = true, message = "Белки не могут быть отрицательными")
        BigDecimal proteins,
        @NotNull(message = "Жиры обязательны")
        @DecimalMin(value = "0.0", inclusive = true, message = "Жиры не могут быть отрицательными")
        BigDecimal fats,
        @NotNull(message = "Углеводы обязательны")
        @DecimalMin(value = "0.0", inclusive = true, message = "Углеводы не могут быть отрицательными")
        BigDecimal carbs,
        @Valid
        @NotEmpty(message = "У блюда должен быть хотя бы один ингредиент")
        List<MealIngredientRequestDto> ingredients
) {}