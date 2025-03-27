package org.slotify.openhourservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import user.Coach;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(
            KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailNotification(Coach coach) {
        try {
            kafkaTemplate.send("email_open_hour_update", coach.toByteArray());
        } catch (Exception e) {
            log.error("Error sending coach with error: {}", e.getMessage());
        }
    }
}
