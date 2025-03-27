package org.slotify.authservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.authservice.exception.ResourceNotFoundException;
import org.slotify.authservice.grpc.UserServiceGrpcClient;
import org.slotify.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import user.UserResponse;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final JwtDecoder jwtDecoder;
    @Value("${cognito.client_id.student}")
    private String CLIENT_ID_STUDENT;
    @Value("${cognito.client_id.coach}")
    private String CLIENT_ID_COACH;


    @Override
    @Transactional
    public String handleGoogleSignIn(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        String email = (String) claims.get("email");
        String name = (String) claims.get("name");
        String picture = (String) claims.get("picture");
        String clientId = ((List<String>) claims.get("aud")).getFirst();

        if (!CLIENT_ID_STUDENT.equals(clientId) && !CLIENT_ID_COACH.equals(clientId)) {
            throw new ResourceNotFoundException("Request source", "client id", clientId);
        }
        String source = CLIENT_ID_STUDENT.equals(clientId) ? "student" : "coach";
        UserResponse response = userServiceGrpcClient.getExistedUserOrCreateNewUser(email, name, picture, source);
        log.info("Received response from user service via GRPC: {}", response);
        return response.getId();
    }

    @Override
    public String validateTokenAndReturnSource(String token) {
        log.info("Validating token: {}", token);
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Map<String, Object> claims = jwt.getClaims();
            String clientId = ((List<String>) claims.get("aud")).getFirst();

            if (!CLIENT_ID_STUDENT.equals(clientId) && !CLIENT_ID_COACH.equals(clientId)) {
                return null;
            }

            return clientId;
        } catch (JwtException e) {
            return null;
        }
    }

}
