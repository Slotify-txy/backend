package org.slotify.emailtokenservice.service;

import com.google.protobuf.Timestamp;

public interface EmailTokenService {
    String generateToken(String slotId, Timestamp startAt);

    boolean validateToken(String token);

    void deleteToken(String token);


    void deleteTokenBySlot(String slotId);
}
