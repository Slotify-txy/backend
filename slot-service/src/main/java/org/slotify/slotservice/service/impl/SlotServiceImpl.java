package org.slotify.slotservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.slotservice.constant.FrontendSource;
import org.slotify.slotservice.constant.SlotStatus;
import org.slotify.slotservice.entity.Slot;
import org.slotify.slotservice.exception.ResourceNotFoundException;
import org.slotify.slotservice.exception.SlotStatusStaleException;
import org.slotify.slotservice.grpc.EmailTokenServiceGrpcClient;
import org.slotify.slotservice.grpc.UserServiceGrpcClient;
import org.slotify.slotservice.repository.SlotRepository;
import org.slotify.slotservice.service.SlotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import user.Coach;
import user.Student;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlotServiceImpl implements SlotService {
    @Value("${spring.cloud.aws.sns.topic.arn}")
    private String topicArn;

    private final SlotRepository slotRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final EmailTokenServiceGrpcClient emailTokenServiceGrpcClient;
    private static final Logger log = LoggerFactory.getLogger(SlotServiceImpl.class);
    private final SnsPublisher snsPublisher;

    @Override
    public List<Slot> getSlotsByStudentIdAndCoachId(UUID studentId, UUID coachId, FrontendSource source) {
        return slotRepository.findSlotsByStudentIdAndCoachId(studentId, coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "studentId/coachId", studentId + "/" + coachId))
                .stream()
                .filter(slot -> source == FrontendSource.COACH ? !slot.getCoachDeleted() : !slot.getStudentDeleted())
                .toList();
    }

    @Override
    public Slot getSlotById(UUID id) {
        return slotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Slot", "id", String.valueOf(id)));
    }

    @Override
    @Transactional
    public List<Slot> createSlots(List<Slot> slots) {
        // All slots should be in the same status. If not AVAILABLE, then it should already have a class id
        if (!slots.isEmpty() && slots.get(0).getStatus().equals(SlotStatus.AVAILABLE)) {
            UUID classId = UUID.randomUUID();
            slots.forEach(slot -> slot.setClassId(classId));
        }
        List<Slot> newSlots = slotRepository.saveAll(slots);

        newSlots.forEach(slot -> {
            if (slot.getStatus().equals(SlotStatus.PENDING)) {
                snsPublisher.publish(topicArn, convertToGrpcSlot(slot));
            }
        });
        return newSlots;
    }

    @Override
    public List<Slot> getSlotsByCoachId(UUID coachId, FrontendSource source) {
        return slotRepository.findSlotsByCoachId(coachId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot", "coachId", String.valueOf(coachId)))
                .stream()
                .filter(slot -> source == FrontendSource.COACH ? !slot.getCoachDeleted() : !slot.getStudentDeleted())
                .toList();
    }

    @Override
    @Transactional
    public void deleteSlotById(UUID id, FrontendSource source) {
        Slot slot = getSlotById(id);

        // Deletion can be triggered by mistake for APPOINTMENT slots when the user doesn't get the latest status
        if (slot.getStatus().equals(SlotStatus.APPOINTMENT)) {
            throw new SlotStatusStaleException(id);
        }

        // Soft deletion for cancelled and rejected slots, otherwise the other side won't be able to see the cancelled slots
        if (slot.getStatus().equals(SlotStatus.CANCELLED) || slot.getStatus().equals(SlotStatus.REJECTED)) {
            boolean isAdmin = source == FrontendSource.COACH;
            // For admin, check if the student has already deleted; for client, check if the coach has already deleted.
            boolean otherSideDeleted = isAdmin ? slot.getStudentDeleted() : slot.getCoachDeleted();

            if (otherSideDeleted) {
                slotRepository.deleteById(id);
            } else {
                if (isAdmin) {
                    slot.setCoachDeleted(true);
                } else {
                    slot.setStudentDeleted(true);
                }
                slotRepository.saveAndFlush(slot);
            }
            return;
        }

        emailTokenServiceGrpcClient.deleteTokenBySlotId(slot.getId());
        slotRepository.deleteById(id);
    }

    @Override
    public Slot updateSlotStatus(UUID id, SlotStatus status) {
        Slot slot = slotRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Slot", "id", String.valueOf(id)));
        if (!status.isAfter(slot.getStatus())) {
            throw new SlotStatusStaleException(id);
        }
        slot.setStatus(status);
        Slot updatedSlot = slotRepository.save(slot);

        emailTokenServiceGrpcClient.deleteTokenBySlotId(slot.getId());

        snsPublisher.publish(topicArn, convertToGrpcSlot(slot));

        return updatedSlot;
    }


    private slot.Slot convertToGrpcSlot(Slot slotEntity) {
        Student student = userServiceGrpcClient.getStudentById(slotEntity.getStudentId());
        Coach coach = userServiceGrpcClient.getCoachById(slotEntity.getCoachId());
        Instant startAtInstant = slotEntity.getStartAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant endAtInstant = slotEntity.getEndAt().atZone(ZoneId.systemDefault()).toInstant();

        Timestamp startAt = Timestamp.newBuilder()
                .setSeconds(startAtInstant.getEpochSecond())
                .setNanos(startAtInstant.getNano())
                .build();

        Timestamp endAt = Timestamp.newBuilder()
                .setSeconds(endAtInstant.getEpochSecond())
                .setNanos(endAtInstant.getNano())
                .build();

        return slot.Slot.newBuilder()
                .setId(slotEntity.getId().toString())
                .setStartAt(startAt)
                .setEndAt(endAt)
                .setClassId(slotEntity.getClassId().toString())
                .setCoach(coach)
                .setStudent(student)
                .setStatus(slotEntity.getStatus().mapToProtoStatus())
                .build();
    }

    @Override
    public String updateSlotStatusViaEmail(UUID id, String token, SlotStatus status) {
        log.info("Updating slot status via email: [id = {}, token = {}, status = {}]", id, token, status);
        if (!emailTokenServiceGrpcClient.validateToken(token)) {
            return null;
        }
        emailTokenServiceGrpcClient.deleteToken(token);
        updateSlotStatus(id, status);
        return switch (status) {
            case APPOINTMENT -> "Appointment Confirmed!";
            case REJECTED -> "Appointment Rejected!";
            case CANCELLED -> "Appointment Cancelled!";
            default -> null;
        };
    }
}
