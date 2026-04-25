package ru.iposhka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iposhka.dto.event.CoupleInviteAcceptedEvent;

@Service
@RequiredArgsConstructor
public class CoupleInviteAcceptedEventPublisher {

    private final KafkaTemplate<String, CoupleInviteAcceptedEvent> kafkaTemplate;

    @Value("${couple.invite.accepted.topic}")
    private String topic;

    public void publish(CoupleInviteAcceptedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.inviterUserId()), event);
    }
}