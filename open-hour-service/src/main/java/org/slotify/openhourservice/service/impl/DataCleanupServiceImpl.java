package org.slotify.openhourservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.slotify.openhourservice.repository.OpenHourRepository;
import org.slotify.openhourservice.service.DataCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DataCleanupServiceImpl implements DataCleanupService {
    private final OpenHourRepository openHourRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 ? * SUN")
    public void deleteOldDataWeekly() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusWeeks(1);
        openHourRepository.deleteOpenHoursByEndAtBefore(cutoffDate);
    }
}
