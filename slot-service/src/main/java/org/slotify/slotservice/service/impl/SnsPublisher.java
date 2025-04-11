package org.slotify.slotservice.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@RequiredArgsConstructor
public class SnsPublisher {
    private final SnsClient snsClient;
    private static final Logger log = LoggerFactory.getLogger(SnsPublisher.class);

    public void publish(String topicArn, MessageOrBuilder payload) {
        try {
            String message = JsonFormat.printer().print(payload);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();
            PublishResponse response = snsClient.publish(request);

            log.info("Message published. id: {}", response.messageId());
        } catch (InvalidProtocolBufferException e) {
            log.error("Fail to convert to json, {}", e.getMessage());
        }
    }
}
