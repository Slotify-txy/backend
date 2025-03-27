package org.slotify.slotservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import slot.Slot;

@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(
            KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailNotification(Slot slot) {
        try {
            kafkaTemplate.send("email_slot_status_update", slot.toByteArray());
        } catch (Exception e) {
            log.error("Error sending slots with error: {}", e.getMessage());
        }
    }
}
