package ru.iposhka.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "email_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailJob {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "code_id", nullable = false, updatable = false)
    private UUID codeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EmailJobStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    private EmailJob(
            UUID eventId,
            UUID codeId,
            Long userId,
            String email,
            String subject,
            String body,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime updatedAt
    ) {
        this.eventId = eventId;
        this.codeId = codeId;
        this.userId = userId;
        this.email = email;
        this.subject = subject;
        this.body = body;
        this.status = EmailJobStatus.PENDING;
        this.attempts = 0;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public static EmailJob pending(
            UUID eventId,
            UUID codeId,
            Long userId,
            String email,
            String subject,
            String body,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            LocalDateTime updatedAt
    ) {
        return new EmailJob(eventId, codeId, userId, email, subject, body, createdAt, expiresAt, updatedAt);
    }

    public void markProcessing(LocalDateTime now) {
        this.status = EmailJobStatus.PROCESSING;
        this.updatedAt = now;
        this.errorMessage = null;
    }

    public void markSent(LocalDateTime now) {
        this.status = EmailJobStatus.SENT;
        this.attempts += 1;
        this.sentAt = now;
        this.updatedAt = now;
        this.nextAttemptAt = null;
        this.errorMessage = null;
    }

    public void markForRetry(LocalDateTime now, LocalDateTime nextAttemptAt, String errorMessage) {
        this.status = EmailJobStatus.PENDING;
        this.attempts += 1;
        this.updatedAt = now;
        this.nextAttemptAt = nextAttemptAt;
        this.errorMessage = errorMessage;
    }

    public void markFailed(LocalDateTime now, String errorMessage) {
        this.status = EmailJobStatus.FAILED;
        this.attempts += 1;
        this.updatedAt = now;
        this.nextAttemptAt = null;
        this.errorMessage = errorMessage;
    }

    public void markExpired(LocalDateTime now, String errorMessage) {
        this.status = EmailJobStatus.EXPIRED;
        this.updatedAt = now;
        this.nextAttemptAt = null;
        this.errorMessage = errorMessage;
    }

    public boolean isExpiredAt(LocalDateTime moment) {
        return !expiresAt.isAfter(moment);
    }
}