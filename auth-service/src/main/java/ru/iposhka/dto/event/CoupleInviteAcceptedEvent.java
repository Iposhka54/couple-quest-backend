package ru.iposhka.dto.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoupleInviteAcceptedEvent(
        UUID eventId,
        Long inviterUserId,
        String inviterEmail,
        String inviterName,
        Long accepterUserId,
        String accepterEmail,
        String accepterName,
        LocalDateTime createdAt
) {
}