package org.slotify.slotservice.grpc;

import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.slotservice.exception.ResourceNotFoundException;
import org.slotify.slotservice.repository.SlotRepository;
import slot.SlotServiceGrpc.SlotServiceImplBase;
import slot.StudentAndCount;
import slot.StudentAndCounts;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@GrpcService
public class SlotGrpcService extends SlotServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(
            SlotGrpcService.class);
    private final SlotRepository slotRepository;

    @Override
    public void getAvailableStudents(StringValue coachIdValue,
                                     StreamObserver<StudentAndCounts> responseObserver) {
        String coachId = coachIdValue.getValue();
        log.info("getAvailableStudents request received {}", coachId);
        List<StudentAndCount> studentAndCounts = slotRepository.findAvailableStudents(UUID.fromString(coachId))
                .orElseThrow(() -> new ResourceNotFoundException("Student", "coachId", coachId))
                .stream()
                .map(row -> StudentAndCount.newBuilder()
                        .setStudentId(row[0].toString())
                        .setCount(Integer.parseInt(row[1].toString()))
                        .build())
                .toList();

        StudentAndCounts response = StudentAndCounts.newBuilder()
                .addAllStudentAndCount(studentAndCounts)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
