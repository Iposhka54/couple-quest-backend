package ru.iposhka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.service.EmailNotificationService;
import ru.iposhka.util.EmailUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationListener {

    private final EmailNotificationService emailNotificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.email-verification}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handle(EmailVerificationRequestedEvent event) {
        if (event == null) {
            return;
        }
        log.info("Received email verification event: eventId={}, userId={}, email={}",
                event.eventId(),
                event.userId(),
                EmailUtils.maskEmail(event.email())
        );

        emailNotificationService.sendVerificationCode(event);
    }
}