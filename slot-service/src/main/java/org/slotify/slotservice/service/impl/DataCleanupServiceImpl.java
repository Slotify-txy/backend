package org.slotify.slotservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.slotify.slotservice.repository.SlotRepository;
import org.slotify.slotservice.service.DataCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DataCleanupServiceImpl implements DataCleanupService {
    private final SlotRepository slotRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 ? * SUN")
    public void deleteOldDataWeekly() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusWeeks(1);
        slotRepository.deleteSlotsByEndAtBefore(cutoffDate);
    }
}
