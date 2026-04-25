package ru.iposhka.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.iposhka.dto.event.CoupleInviteAcceptedEvent;
import ru.iposhka.config.MailProperties;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.exception.EmailDeliveryException;
import ru.iposhka.model.EmailJob;
import ru.iposhka.util.EmailUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {
    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailTemplateLoader templateLoader;

    @Value("${app.mail.template.verification-code}")
    private String verificationTemplatePath;

    @Value("${app.mail.template.couple-invite-accepted}")
    private String coupleInviteAcceptedTemplatePath;

    @Value("${app.mail.site-url}")
    private String siteUrl;

    public EmailContent prepareVerificationEmail(EmailVerificationRequestedEvent event) {
        validateEvent(event);

        return new EmailContent(buildSubject(), buildHtml(event));
    }

    public EmailContent prepareCoupleInviteAcceptedEmail(CoupleInviteAcceptedEvent event) {
        validateInviteAcceptedEvent(event);

        return new EmailContent(buildInviteAcceptedSubject(), buildInviteAcceptedHtml(event));
    }

    public void send(EmailJob job) {
        Objects.requireNonNull(job, "EmailJob must not be null");
        requireNonBlank(job.getEmail(), "Recipient email must not be blank");
        requireNonBlank(job.getSubject(), "Email subject must not be blank");
        requireNonBlank(job.getBody(), "Email body must not be blank");

        if (!SIMPLE_EMAIL_PATTERN.matcher(job.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Recipient email has invalid format");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(mailProperties.from());
            helper.setTo(job.getEmail().trim());
            helper.setSubject(job.getSubject());
            helper.setText(job.getBody(), true);

            mailSender.send(mimeMessage);

            log.info(
                    "Verification email sent successfully: eventId={}, userId={}, email={}, expiresAt={}",
                    job.getEventId(),
                    job.getUserId(),
                    EmailUtils.maskEmail(job.getEmail()),
                    job.getExpiresAt()
            );
        } catch (MailAuthenticationException ex) {
            log.error(
                    "Mail authentication failed: eventId={}, userId={}, email={}",
                    job.getEventId(),
                    job.getUserId(),
                    EmailUtils.maskEmail(job.getEmail()),
                    ex
            );
            throw new EmailDeliveryException("SMTP authentication failed", ex);
        } catch (MessagingException | MailException ex) {
            log.error(
                    "Failed to send verification email: eventId={}, userId={}, email={}",
                    job.getEventId(),
                    job.getUserId(),
                    EmailUtils.maskEmail(job.getEmail()),
                    ex
            );
            throw new EmailDeliveryException("Failed to send verification email", ex);
        }
    }

    private String buildSubject() {
        return "Подтверждение email в " + mailProperties.projectName();
    }

    private String buildHtml(EmailVerificationRequestedEvent event) {
        String template = templateLoader.loadTemplate(verificationTemplatePath);

        return template
                .replace("{{projectName}}", Encode.forHtml(mailProperties.projectName()))
                .replace("{{username}}", Encode.forHtml(resolveUserName(event.name())))
                .replace("{{code}}", Encode.forHtml(event.code().trim()))
                .replace("{{expiresAt}}", Encode.forHtml(formatDateTime(event.expiresAt())))
                .replace("{{supportEmail}}", Encode.forHtml(mailProperties.supportEmail()));
    }

    private String buildInviteAcceptedSubject() {
        return "Ваше приглашение в пару принято в " + mailProperties.projectName();
    }

    private String buildInviteAcceptedHtml(CoupleInviteAcceptedEvent event) {
        String template = templateLoader.loadTemplate(coupleInviteAcceptedTemplatePath);

        return template
                .replace("{{projectName}}", Encode.forHtml(mailProperties.projectName()))
                .replace("{{inviterName}}", Encode.forHtml(resolveUserName(event.inviterName())))
                .replace("{{partnerName}}", Encode.forHtml(resolveUserName(event.accepterName())))
                .replace("{{partnerEmail}}", Encode.forHtml(event.accepterEmail().trim()))
                .replace("{{acceptedAt}}", Encode.forHtml(formatDateTime(event.createdAt())))
                .replace("{{siteUrl}}", Encode.forHtmlAttribute(siteUrl))
                .replace("{{siteUrlText}}", Encode.forHtml(siteUrl))
                .replace("{{supportEmail}}", Encode.forHtml(mailProperties.supportEmail()));
    }

    private String resolveUserName(String name) {
        if (name == null || name.isBlank()) {
            return "гость";
        }
        return name.trim();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "не указано";
        }
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private void validateEvent(EmailVerificationRequestedEvent event) {
        Objects.requireNonNull(event, "EmailVerificationRequestedEvent must not be null");
        requireNonBlank(event.email(), "Recipient email must not be blank");
        requireNonBlank(event.code(), "Verification code must not be blank");

        if (!SIMPLE_EMAIL_PATTERN.matcher(event.email().trim()).matches()) {
            throw new IllegalArgumentException("Recipient email has invalid format");
        }
    }

    private void validateInviteAcceptedEvent(CoupleInviteAcceptedEvent event) {
        Objects.requireNonNull(event, "CoupleInviteAcceptedEvent must not be null");
        requireNonBlank(event.inviterEmail(), "Recipient email must not be blank");
        requireNonBlank(event.accepterEmail(), "Partner email must not be blank");

        if (!SIMPLE_EMAIL_PATTERN.matcher(event.inviterEmail().trim()).matches()) {
            throw new IllegalArgumentException("Recipient email has invalid format");
        }

        if (!SIMPLE_EMAIL_PATTERN.matcher(event.accepterEmail().trim()).matches()) {
            throw new IllegalArgumentException("Partner email has invalid format");
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public record EmailContent(String subject, String body) {
    }
}