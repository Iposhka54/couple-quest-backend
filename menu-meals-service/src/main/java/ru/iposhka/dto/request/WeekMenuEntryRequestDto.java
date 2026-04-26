package ru.iposhka.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ru.iposhka.model.MealType;

public record WeekMenuEntryRequestDto(
        @NotNull(message = "Дата блюда обязательна")
        LocalDate plannedDate,
        @NotNull(message = "Тип приема пищи обязателен")
        MealType mealType,
        @NotNull(message = "mealId обязателен")
        Long mealId,
        @NotNull(message = "Количество порций обязательно")
        @Min(value = 1, message = "Количество порций должно быть минимум 1")
        @Max(value = 20, message = "Количество порций не должно быть больше 20")
        Integer servings,
        String note
) {}