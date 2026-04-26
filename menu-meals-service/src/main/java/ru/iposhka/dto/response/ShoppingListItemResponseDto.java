package ru.iposhka.dto.response;

import java.math.BigDecimal;

public record ShoppingListItemResponseDto(
        String name,
        BigDecimal totalAmount,
        String unit
) {}