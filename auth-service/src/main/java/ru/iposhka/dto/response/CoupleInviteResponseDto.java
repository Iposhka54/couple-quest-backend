package ru.iposhka.dto.response;

import java.time.LocalDateTime;

public record CoupleInviteResponseDto(
        String inviteId,
        Long coupleId,
        String token,
        String expectedGender,
        String status,
        LocalDateTime expiresAt
) {}