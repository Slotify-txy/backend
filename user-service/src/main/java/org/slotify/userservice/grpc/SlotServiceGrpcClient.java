package org.slotify.userservice.grpc;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import slot.SlotServiceGrpc;
import slot.SlotServiceGrpc.SlotServiceBlockingStub;
import slot.StudentAndCounts;

import java.util.UUID;

@Service
public class SlotServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(
            SlotServiceGrpcClient.class);
    private final SlotServiceBlockingStub blockingStub;

    public SlotServiceGrpcClient(
            @Value("${slot.service.address:localhost}") String serverAddress,
            @Value("${slot.service.grpc.port:9002}") int serverPort) {

        log.info("Connecting to slot service GRPC service at {}:{}",
                serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress,
                serverPort).usePlaintext().build();

        blockingStub = SlotServiceGrpc.newBlockingStub(channel);
    }

    public StudentAndCounts getAvailableStudents(UUID coachId) {
        log.info("getAvailableStudents invoked");
        StringValue request = StringValue.newBuilder()
                .setValue(coachId.toString())
                .build();

        StudentAndCounts response = blockingStub.getAvailableStudents(request);
        log.info("Received response from slot service via GRPC: {}", response);
        return response;
    }
}
