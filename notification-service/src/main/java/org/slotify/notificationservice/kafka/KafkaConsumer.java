package org.slotify.notificationservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.notificationservice.service.impl.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import slot.Slot;
import user.Coach;

@Service
@RequiredArgsConstructor
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(
            KafkaConsumer.class);

    private final EmailService emailService;

    @KafkaListener(topics="email_open_hour_update", groupId = "open-hour-update-notification")
    public void handleOpenHourUpdate(byte[] event) {
        try {
            Coach coach = Coach.parseFrom(event);
            log.info("Received Coach: [id={}, name={}, email={}]",
                    coach.getId(),
                    coach.getName(),
                    coach.getEmail());

            emailService.sendOpenHourUpdateEmail(coach);

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @KafkaListener(topics="email_slot_status_update", groupId = "slot-update-notification")
    public void handleSlotStatusUpdate(byte[] event) {
        try {
            Slot slot = Slot.parseFrom(event);

            log.info("Received Slot: id={}", slot.getId());

            emailService.sendSlotStatusUpdateEmail(slot);

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
