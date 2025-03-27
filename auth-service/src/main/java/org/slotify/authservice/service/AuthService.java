package org.slotify.authservice.service;

import org.springframework.security.oauth2.jwt.Jwt;


public interface AuthService {
    String handleGoogleSignIn(Jwt jwt);

    String validateTokenAndReturnSource(String token);
}
