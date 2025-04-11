package org.slotify.emailtokenservice.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slotify.emailtokenservice.service.EmailTokenService;
import slot.TokenGenerationRequest;
import slot.TokenServiceGrpc.TokenServiceImplBase;

@RequiredArgsConstructor
@GrpcService
public class EmailTokenGrpcService extends TokenServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(
            EmailTokenGrpcService.class);
    private final EmailTokenService emailTokenService;

    @Override
    public void generateToken(TokenGenerationRequest tokenGenerationRequest,
                              StreamObserver<StringValue> responseObserver) {
        String slotId = tokenGenerationRequest.getId();
        Timestamp endAt = tokenGenerationRequest.getStartAt();

        log.info("generateToken request received, [slot_id: {}, end_at: {}]", slotId, endAt);
        String token = emailTokenService.generateToken(slotId, endAt);

        StringValue response = StringValue.newBuilder()
                .setValue(token)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(StringValue tokenValue,
                              StreamObserver<BoolValue> responseObserver) {
        String token = tokenValue.getValue();

        log.info("validateToken request received, token: {}", token);
        boolean isValid = emailTokenService.validateToken(token);

        BoolValue response = BoolValue.newBuilder()
                .setValue(isValid)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteToken(StringValue tokenValue,
                              StreamObserver<Empty> responseObserver) {
        String token = tokenValue.getValue();

        log.info("deleteToken request received, token: {}", token);
        emailTokenService.deleteToken(token);

        Empty response = Empty.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteTokenBySlotId(StringValue slotIdValue,
                            StreamObserver<Empty> responseObserver) {
        String slotId = slotIdValue.getValue();

        log.info("deleteTokenBySlotId request received, slot_id: {}", slotId);
        emailTokenService.deleteTokenBySlot(slotId);

        Empty response = Empty.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
