package ru.iposhka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;
import ru.iposhka.service.EmailJobService;
import ru.iposhka.util.EmailUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationListener {

    private final EmailJobService emailJobService;

    @KafkaListener(
            topics = "${app.kafka.topics.email-verification}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handle(EmailVerificationRequestedEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            acknowledgment.acknowledge();
            return;
        }

        log.info("Received email verification event: eventId={}, userId={}, email={}",
                event.eventId(),
                event.userId(),
                EmailUtils.maskEmail(event.email())
        );

        emailJobService.createPendingJobIfNotExists(event);
        acknowledgment.acknowledge();
    }
}