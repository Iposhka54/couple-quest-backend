package ru.iposhka.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.iposhka.dto.event.CoupleInviteAcceptedEvent;
import ru.iposhka.service.EmailJobService;
import ru.iposhka.util.EmailUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoupleInviteAcceptedListener {

    private final EmailJobService emailJobService;

    @KafkaListener(
            topics = "${app.kafka.topics.couple-invite-accepted}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handle(CoupleInviteAcceptedEvent event, Acknowledgment acknowledgment) {
        if (event == null) {
            acknowledgment.acknowledge();
            return;
        }

        log.info("Received couple invite accepted event: eventId={}, inviterUserId={}, inviterEmail={}, accepterUserId={}",
                event.eventId(),
                event.inviterUserId(),
                EmailUtils.maskEmail(event.inviterEmail()),
                event.accepterUserId()
        );

        emailJobService.createPendingJobIfNotExists(event);
        acknowledgment.acknowledge();
    }
}