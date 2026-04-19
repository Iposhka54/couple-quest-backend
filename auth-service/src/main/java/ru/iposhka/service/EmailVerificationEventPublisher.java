package ru.iposhka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.iposhka.dto.event.EmailVerificationRequestedEvent;

@Service
@RequiredArgsConstructor
public class EmailVerificationEventPublisher {

    private final KafkaTemplate<String, EmailVerificationRequestedEvent> kafkaTemplate;

    @Value("${email-verification.topic}")
    private String topic;

    public void publish(EmailVerificationRequestedEvent event) {
        kafkaTemplate.send(topic, event.email(), event);
    }
}