package org.slotify.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.slotify.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class OAuth2Controller {

    private final AuthService authService;

    @PostMapping("/sign-in/google")
    public ResponseEntity<Map<String, String>> createGoogleUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String id = authService.handleGoogleSignIn(jwt);
            return new ResponseEntity<>(Map.of(
                    "token", jwt.getTokenValue(),
                    "id", id
            ), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                    "message", e.getMessage()
            ), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validate(@RequestHeader("Authorization") String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String source = authService.validateTokenAndReturnSource(authHeader.substring(7));

        if (source == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return new ResponseEntity<>(source, HttpStatus.OK);
    }
}