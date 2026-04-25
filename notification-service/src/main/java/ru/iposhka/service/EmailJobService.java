package ru.iposhka.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.iposhka.config.EmailJobProperties;
import ru.iposhka.dto.event.CoupleInviteAcceptedEvent;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.model.EmailJob;
import ru.iposhka.repository.EmailJobRepository;
import ru.iposhka.util.EmailUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailJobService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final EmailJobRepository emailJobRepository;
    private final EmailNotificationService emailNotificationService;
    private final EmailJobProperties properties;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public void createPendingJobIfNotExists(EmailVerificationRequestedEvent event) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("Email verification event and eventId must not be null");
        }

        if (event.codeId() == null) {
            throw new IllegalArgumentException("Email verification event codeId must not be null");
        }

        if (!"ACTIVE".equalsIgnoreCase(event.status())) {
            log.info("Skipping email job creation because verification status is not ACTIVE: eventId={}, codeId={}, status={}",
                    event.eventId(), event.codeId(), event.status());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (event.expiresAt() == null || !event.expiresAt().isAfter(now)) {
            log.info("Skipping expired email verification event: eventId={}, codeId={}, userId={}, email={}, expiresAt={}",
                    event.eventId(), event.codeId(), event.userId(), EmailUtils.maskEmail(event.email()), event.expiresAt());
            return;
        }

        if (emailJobRepository.existsById(event.eventId())) {
            log.info("Email job already exists, skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        EmailNotificationService.EmailContent content = emailNotificationService.prepareVerificationEmail(event);
        LocalDateTime createdAt = Optional.ofNullable(event.createdAt()).orElse(now);

        EmailJob job = EmailJob.pending(
                event.eventId(),
                event.codeId(),
                event.userId(),
                event.email().trim(),
                content.subject(),
                content.body(),
                createdAt,
                event.expiresAt(),
                now
        );

        try {
            emailJobRepository.save(job);
            log.info(
                    "Email job created: eventId={}, userId={}, email={}, status={}",
                    event.eventId(),
                    event.userId(),
                    EmailUtils.maskEmail(event.email()),
                    job.getStatus()
            );
        } catch (DataIntegrityViolationException ex) {
            log.info("Email job already persisted concurrently, skipping duplicate event: eventId={}", event.eventId());
        }
    }

    @Transactional
    public void createPendingJobIfNotExists(CoupleInviteAcceptedEvent event) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("Couple invite accepted event and eventId must not be null");
        }

        if (emailJobRepository.existsById(event.eventId())) {
            log.info("Email job already exists, skipping duplicate event: eventId={}", event.eventId());
            return;
        }

        EmailNotificationService.EmailContent content =
                emailNotificationService.prepareCoupleInviteAcceptedEmail(event);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = Optional.ofNullable(event.createdAt()).orElse(now);

        EmailJob job = EmailJob.pending(
                event.eventId(),
                event.eventId(),
                event.inviterUserId(),
                event.inviterEmail().trim(),
                content.subject(),
                content.body(),
                createdAt,
                createdAt.plusDays(7),
                now
        );

        try {
            emailJobRepository.save(job);
            log.info(
                    "Couple invite accepted email job created: eventId={}, inviterUserId={}, inviterEmail={}, status={}",
                    event.eventId(),
                    event.inviterUserId(),
                    EmailUtils.maskEmail(event.inviterEmail()),
                    job.getStatus()
            );
        } catch (DataIntegrityViolationException ex) {
            log.info("Email job already persisted concurrently, skipping duplicate event: eventId={}", event.eventId());
        }
    }

    @Transactional
    public List<EmailJob> claimNextBatch(int requestedLimit) {
        if (requestedLimit <= 0) {
            return Collections.emptyList();
        }

        int limit = Math.min(requestedLimit, properties.batchSize());
        List<EmailJob> jobs = emailJobRepository.lockNextBatch(limit, properties.processingTimeoutSeconds());
        if (jobs.isEmpty()) {
            return jobs;
        }

        LocalDateTime now = LocalDateTime.now();
        jobs.forEach(job -> job.markProcessing(now));
        emailJobRepository.saveAll(jobs);

        log.debug("Claimed {} email jobs for processing", jobs.size());
        return jobs;
    }

    public void process(EmailJob job) {
        try {
            if (job.isExpiredAt(LocalDateTime.now())) {
                withTransaction(() -> markExpired(job.getEventId()));
                return;
            }
            emailNotificationService.send(job);
            withTransaction(() -> markSent(job.getEventId()));
        } catch (Exception ex) {
            withTransaction(() -> markFailure(job.getEventId(), ex));
        }
    }

    private void markSent(UUID eventId) {
        EmailJob job = emailJobRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Email job not found: " + eventId));

        LocalDateTime now = LocalDateTime.now();
        job.markSent(now);
        emailJobRepository.save(job);

        log.info("Email job completed successfully: eventId={}, userId={}, attempts={}",
                job.getEventId(),
                job.getUserId(),
                job.getAttempts());
    }

    private void markFailure(UUID eventId, Exception exception) {
        EmailJob job = emailJobRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Email job not found: " + eventId));

        LocalDateTime now = LocalDateTime.now();
        String message = truncateErrorMessage(exception);
        int nextAttemptNumber = job.getAttempts() + 1;

        if (nextAttemptNumber >= properties.maxAttempts()) {
            job.markFailed(now, message);
            log.error("Email job failed permanently: eventId={}, userId={}, attempts={}, email={}",
                    job.getEventId(),
                    job.getUserId(),
                    job.getAttempts(),
                    EmailUtils.maskEmail(job.getEmail()),
                    exception);
        } else {
            long delaySeconds = resolveRetryDelaySeconds(nextAttemptNumber);
            job.markForRetry(now, now.plusSeconds(delaySeconds), message);
            log.warn("Email job scheduled for retry: eventId={}, userId={}, attempt={}, nextAttemptAt={}, email={}",
                    job.getEventId(),
                    job.getUserId(),
                    job.getAttempts(),
                    job.getNextAttemptAt(),
                    EmailUtils.maskEmail(job.getEmail()),
                    exception);
        }

        emailJobRepository.save(job);
    }

    private void markExpired(UUID eventId) {
        EmailJob job = emailJobRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Email job not found: " + eventId));

        LocalDateTime now = LocalDateTime.now();
        job.markExpired(now, "Verification code expired before email delivery");
        emailJobRepository.save(job);

        log.info("Email job marked expired without sending: eventId={}, codeId={}, userId={}, email={}",
                job.getEventId(),
                job.getCodeId(),
                job.getUserId(),
                EmailUtils.maskEmail(job.getEmail()));
    }

    private long resolveRetryDelaySeconds(int attemptNumber) {
        List<Long> retryDelays = properties.retryDelaysSeconds();
        int index = Math.max(0, Math.min(attemptNumber - 1, retryDelays.size() - 1));
        return retryDelays.get(index);
    }

    private String truncateErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private void withTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> action.run());
    }
}