package org.slotify.emailtokenservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.slotify.emailtokenservice.repository.EmailTokenRepository;
import org.slotify.emailtokenservice.service.DataCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DataCleanupServiceImpl implements DataCleanupService {
    private final EmailTokenRepository emailTokenRepository;

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void deleteOldDataWeekly() {
        LocalDateTime now = LocalDateTime.now();
        emailTokenRepository.deleteEmailTokensByExpirationTimeBefore(now);
    }
}
