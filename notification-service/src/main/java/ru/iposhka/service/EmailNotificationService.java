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
import ru.iposhka.config.MailProperties;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.exception.EmailDeliveryException;
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

    public void sendVerificationCode(EmailVerificationRequestedEvent event) {
        validateEvent(event);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(mailProperties.from());
            helper.setTo(event.email().trim());
            helper.setSubject(buildSubject());
            helper.setText(buildHtml(event), true);

            mailSender.send(mimeMessage);

            log.info(
                    "Verification email sent successfully: eventId={}, userId={}, email={}, expiresAt={}",
                    event.eventId(),
                    event.userId(),
                    EmailUtils.maskEmail(event.email()),
                    event.expiresAt()
            );
        } catch (MailAuthenticationException ex) {
            log.error(
                    "Mail authentication failed: eventId={}, userId={}, email={}",
                    event.eventId(),
                    event.userId(),
                    EmailUtils.maskEmail(event.email()),
                    ex
            );
            throw new EmailDeliveryException("SMTP authentication failed", ex);
        } catch (MessagingException | MailException ex) {
            log.error(
                    "Failed to send verification email: eventId={}, userId={}, email={}",
                    event.eventId(),
                    event.userId(),
                    EmailUtils.maskEmail(event.email()),
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

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}