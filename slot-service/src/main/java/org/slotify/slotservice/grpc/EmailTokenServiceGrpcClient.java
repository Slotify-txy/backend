package org.slotify.slotservice.grpc;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slot.TokenServiceGrpc;
import slot.TokenServiceGrpc.TokenServiceBlockingStub;

import java.util.UUID;


@Service
public class EmailTokenServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(
            EmailTokenServiceGrpcClient.class);
    private final TokenServiceBlockingStub blockingStub;

    public EmailTokenServiceGrpcClient(
            @Value("${email-token-service.address:localhost}") String serverAddress,
            @Value("${email-token-service.grpc.port:9003}") int serverPort) {

        log.info("Connecting to email token service GRPC service at {}:{}",
                serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
                serverPort).usePlaintext().build();

        blockingStub = TokenServiceGrpc.newBlockingStub(channel);
    }

    public boolean validateToken(String token) {
        StringValue request = StringValue
                .newBuilder()
                .setValue(token)
                .build();

        return blockingStub.validateToken(request).getValue();
    }

    public void deleteToken(String token) {
        log.info("deleteToken: token = {}", token);
        StringValue request = StringValue
                .newBuilder()
                .setValue(token)
                .build();

        blockingStub.deleteToken(request);
    }

    public void deleteTokenBySlotId(UUID slotId) {
        StringValue request = StringValue
                .newBuilder()
                .setValue(slotId.toString())
                .build();

        blockingStub.deleteTokenBySlotId(request);
    }
}
