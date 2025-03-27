package org.slotify.authservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import user.UserRequest;
import user.UserResponse;
import user.UserServiceGrpc;
import user.UserServiceGrpc.UserServiceBlockingStub;

@Service
public class UserServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(
            UserServiceGrpcClient.class);
    private final UserServiceBlockingStub blockingStub;

    public UserServiceGrpcClient(
            @Value("${user-service.address:localhost}") String serverAddress,
            @Value("${user-service.grpc.port:9001}") int serverPort) {

        log.info("Connecting to user service GRPC service at {}:{}",
                serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
                serverPort).usePlaintext().build();

        blockingStub = UserServiceGrpc.newBlockingStub(channel);
    }

    public UserResponse getExistedUserOrCreateNewUser(String email, String name,
                                             String picture, String source) {
        log.info("getExistedUserOrCreateNewUser invoked");
        UserRequest request = UserRequest.newBuilder()
                .setPicture(picture)
                .setName(name)
                .setEmail(email)
                .setSource(source)
                .build();

        UserResponse response = blockingStub.getExistedUserOrCreateNewUser(request);
        log.info("Received response from user service via GRPC: {}", response);
        return response;
    }
}
