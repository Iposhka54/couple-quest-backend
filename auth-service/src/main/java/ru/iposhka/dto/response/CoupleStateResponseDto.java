package ru.iposhka.dto.response;

public record CoupleStateResponseDto(
        boolean hasCouple,
        String status,
        PartnerShortDto partner
) {}