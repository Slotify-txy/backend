package org.slotify.emailtokenservice.repository;

import org.slotify.emailtokenservice.entity.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {
    Optional<EmailToken> findEmailTokenBySlotId(UUID slotId);

    void deleteEmailTokenBySlotId(UUID slotId);

    void deleteEmailTokensByExpirationTimeBefore(LocalDateTime cutoffDate);
}
