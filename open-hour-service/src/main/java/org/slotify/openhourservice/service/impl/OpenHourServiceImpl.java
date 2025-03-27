package org.slotify.openhourservice.service.impl;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.openhourservice.entity.OpenHour;
import org.slotify.openhourservice.exception.ResourceNotFoundException;
import org.slotify.openhourservice.grpc.UserServiceGrpcClient;
import org.slotify.openhourservice.kafka.KafkaProducer;
import org.slotify.openhourservice.repository.OpenHourRepository;
import org.slotify.openhourservice.service.OpenHourService;
import org.springframework.stereotype.Service;
import user.Coach;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OpenHourServiceImpl implements OpenHourService {
    private static final Logger log = LoggerFactory.getLogger(OpenHourServiceImpl.class);
    private final OpenHourRepository openHourRepository;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    @Override
    public List<OpenHour> getOpenHoursByCoachId(UUID coachId) {
        return openHourRepository.findOpenHoursByCoachId(coachId).orElseThrow(() -> new ResourceNotFoundException("OpenHour", "coachId", String.valueOf(coachId)));
    }

    @Override
    public OpenHour getOpenHourById(UUID id) {
        return openHourRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("OpenHour", "id", String.valueOf(id)));
    }

    @Override
    public List<OpenHour> createOpenHours(UUID coachId, List<OpenHour> openHours) {
        log.info("Creating OpenHours {}", openHours);
        List<OpenHour> savedOpenHours = openHourRepository.saveAll(openHours);
        Coach coach = userServiceGrpcClient.getCoachById(coachId);
        log.info("coach: id {}", coach.getId());
        kafkaProducer.sendEmailNotification(coach);
        log.info("OpenHours saved: {}", savedOpenHours);
        return savedOpenHours;
    }

    @Override
    public void deleteOpenHourById(UUID id) {
        getOpenHourById(id);
        openHourRepository.deleteById(id);
    }

    @Override
    public void deleteOpenHoursByCoachId(UUID coachId) {
        openHourRepository.deleteOpenHoursByCoachId(coachId);
    }
}
