package org.slotify.emailtokenservice.service.impl;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.emailtokenservice.entity.EmailToken;
import org.slotify.emailtokenservice.repository.EmailTokenRepository;
import org.slotify.emailtokenservice.service.EmailTokenService;
import org.slotify.emailtokenservice.util.TimestampConvertor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTokenServiceImpl implements EmailTokenService {
    private static final Logger log = LoggerFactory.getLogger(EmailTokenServiceImpl.class);
    private final EmailTokenRepository emailTokenRepository;

    public String generateToken(String slotId, Timestamp endAt) {
        UUID id = UUID.fromString(slotId);
        EmailToken emailToken = emailTokenRepository.findEmailTokenBySlotId(id).orElse(null);
        if (emailToken != null) {
            return emailToken.getId().toString();
        }
        UUID token = UUID.randomUUID();
        LocalDateTime expirationTime = TimestampConvertor.convertFromProtoTimestampToLocalDateTime(endAt);
        emailToken = new EmailToken(token, id, expirationTime);

        emailTokenRepository.save(emailToken);
        log.info("Created email token: {}", emailToken);

        return token.toString();
    }

    public boolean validateToken(String token) {
        EmailToken emailToken = emailTokenRepository.findById(UUID.fromString(token)).orElse(null);
        return emailToken != null && !emailToken.isExpired();
    }

    public void deleteToken(String token) {
        emailTokenRepository.deleteById(UUID.fromString(token));
        log.info("Deleted token: {}", token);
    }

    @Override
    @Transactional
    public void deleteTokenBySlot(String slotId) {
        emailTokenRepository.deleteEmailTokenBySlotId(UUID.fromString(slotId));
        log.info("Deleted token by slot id, id: {}", slotId);
    }
}
