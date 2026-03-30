package ru.iposhka.dto.response;

import java.time.LocalDateTime;

public record CoupleInviteResponseDto(
        String invite,
        LocalDateTime expiresAt,
        String status
) {}