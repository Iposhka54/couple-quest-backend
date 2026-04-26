package ru.iposhka.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WeekMenuResponseDto(
        Long coupleId,
        LocalDate weekStart,
        LocalDate weekEnd,
        List<WeeklyMenuEntryResponseDto> entries,
        List<ShoppingListItemResponseDto> shoppingList
) {}