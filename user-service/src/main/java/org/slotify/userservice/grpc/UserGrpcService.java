package org.slotify.userservice.grpc;

import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.userservice.entity.user.Coach;
import org.slotify.userservice.entity.user.Student;
import org.slotify.userservice.exception.ResourceNotFoundException;
import org.slotify.userservice.repository.CoachRepository;
import org.slotify.userservice.repository.StudentRepository;
import user.UserRequest;
import user.UserResponse;
import user.UserServiceGrpc.UserServiceImplBase;

import java.util.Random;
import java.util.UUID;

@RequiredArgsConstructor
@GrpcService
public class UserGrpcService extends UserServiceImplBase {
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;
    private static final Logger log = LoggerFactory.getLogger(
            UserGrpcService.class);

    @Override
    public void getExistedUserOrCreateNewUser(UserRequest userRequest,
                                StreamObserver<UserResponse> responseObserver) {

        log.info("getOrCreateUser request received {}", userRequest.toString());
        String source = userRequest.getSource();
        if (!"student".equalsIgnoreCase(source) && !"coach".equalsIgnoreCase(source)) {
            throw new ResourceNotFoundException("Request source", "source", source);
        }

        String returnId;
        String email = userRequest.getEmail();
        String name = userRequest.getName();
        String picture = userRequest.getPicture();
        if ("student".equalsIgnoreCase(source)) {
            Student savedStudent = studentRepository.findByEmail(email).orElseGet(() -> {
                Student student = new Student();
                student.setEmail(email);
                student.setName(name);
                student.setPicture(picture);
                return studentRepository.saveAndFlush(student);
            });
            returnId = savedStudent.getId().toString();
        } else {
            Coach savedCoach = coachRepository.findByEmail(email).orElseGet(() -> {
                Coach coach = new Coach();
                coach.setEmail(email);
                coach.setName(name);
                coach.setPicture(picture);
                Random rand = new Random();
                coach.setInvitationCode(Integer.toString(100000 + rand.nextInt(900000)));
                return coachRepository.saveAndFlush(coach);
            });
            returnId = savedCoach.getId().toString();
        }

        log.info("User retrieved, id: {}", returnId);

        UserResponse response = UserResponse.newBuilder()
                .setId(returnId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCoachById(StringValue coachId,
                             StreamObserver<user.Coach> responseObserver) {
        String id = coachId.getValue();
        log.info("getCoachById request received {}", id);

        Coach coach = coachRepository.findById(UUID.fromString(id)).orElseThrow(() -> new ResourceNotFoundException("Coach", "id", id));

        log.info("Coach retrieved");

        user.Coach response = user.Coach.newBuilder()
                .setId(coach.getId().toString())
                .setEmail(coach.getEmail())
                .setName(coach.getName())
                .addAllStudents(coach.getStudents().stream().map(student ->
                    user.Student.newBuilder()
                        .setId(student.getId().toString())
                        .setEmail(student.getEmail())
                        .setName(student.getName())
                        .build()
                ).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStudentById(StringValue studentId,
                             StreamObserver<user.Student> responseObserver) {
        String id = studentId.getValue();
        log.info("getStudentById request received {}", id);

        Student student = studentRepository.findById(UUID.fromString(id)).orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        log.info("Student retrieved");

        user.Student response = user.Student.newBuilder()
                .setId(student.getId().toString())
                .setEmail(student.getEmail())
                .setName(student.getName())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
