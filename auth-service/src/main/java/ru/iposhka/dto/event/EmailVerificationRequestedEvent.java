package ru.iposhka.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailVerificationRequestedEvent(
        UUID eventId,
        UUID codeId,
        Long userId,
        String email,
        String name,
        String code,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        String status
) {
}