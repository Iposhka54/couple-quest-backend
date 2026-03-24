package ru.iposhka.dto.response;

public record CoupleStateResponseDto(
        boolean hasCouple,
        Long coupleId,
        String status,
        PartnerShortDto partner,
        CoupleInviteResponseDto activeInvite
) {}