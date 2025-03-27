package org.slotify.slotservice.grpc;

import com.google.protobuf.StringValue;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import user.Coach;
import user.Student;
import user.UserServiceGrpc;
import user.UserServiceGrpc.UserServiceBlockingStub;

import java.util.UUID;

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

    public Coach getCoachById(UUID coachId) {
        log.info("getCoachById invoked");
        StringValue request = StringValue.newBuilder()
                .setValue(coachId.toString())
                .build();

        Coach coach = blockingStub.getCoachById(request);
        log.info("Received response from user service via GRPC: {}", coach);
        return coach;
    }

    public Student getStudentById(UUID studentId) {
        log.info("getStudentById invoked");
        StringValue request = StringValue.newBuilder()
                .setValue(studentId.toString())
                .build();

        Student student = blockingStub.getStudentById(request);
        log.info("Received response from user service via GRPC: {}", student);
        return student;
    }
}
