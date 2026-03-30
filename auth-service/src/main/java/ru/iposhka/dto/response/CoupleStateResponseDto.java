package ru.iposhka.dto.response;

public record CoupleStateResponseDto(
        boolean hasCouple,
        PartnerShortDto partner
) {}