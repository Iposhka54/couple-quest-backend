package ru.iposhka.dto.response;

public record CoupleStateResponseDto(
        Long coupleId,
        boolean hasCouple,
        PartnerShortDto partner
) {}