package org.slotify.notificationservice.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slot.TokenGenerationRequest;
import slot.TokenServiceGrpc;
import slot.TokenServiceGrpc.TokenServiceBlockingStub;

@Service
public class EmailTokenServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(
            EmailTokenServiceGrpcClient.class);
    private final TokenServiceBlockingStub blockingStub;

    public EmailTokenServiceGrpcClient(
            @Value("${email_token.service.address:localhost}") String serverAddress,
            @Value("${email_token.service.grpc.port:9003}") int serverPort) {

        log.info("Connecting to email token service GRPC service at {}:{}",
                serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
                serverPort).usePlaintext().build();

        blockingStub = TokenServiceGrpc.newBlockingStub(channel);
    }

    public String generateToken(String slotId, Timestamp startAt) {
        TokenGenerationRequest request = TokenGenerationRequest
                .newBuilder()
                .setId(slotId)
                .setStartAt(startAt)
                .build();

        return blockingStub.generateToken(request).getValue();
    }
}
