package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

@EqualsAndHashCode(callSuper = true)
@Data
public class SNSAndSQSStack extends Stack {
    private final Topic openHourUpdateTopic;
    private final Topic slotStatusUpdateTopic;
    private final Queue openHourUpdateQueue;
    private final Queue slotStatusUpdateQueue;

    public SNSAndSQSStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        // create SNS & SQS
        this.openHourUpdateQueue = Queue.Builder.create(this, "SlotifyOpenHourUpdateQueue")
                .queueName("SlotifyOpenHourUpdateQueue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        this.slotStatusUpdateQueue = Queue.Builder.create(this, "SlotifySlotStatusUpdateQueue")
                .queueName("SlotifySlotStatusUpdateQueue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        this.openHourUpdateTopic = Topic.Builder.create(this, "SlotifyOpenHourUpdateTopic")
                .topicName("SlotifyOpenHourUpdateTopic")
                .build();

        this.slotStatusUpdateTopic = Topic.Builder.create(this, "SlotifySlotStatusUpdateTopic")
                .topicName("SlotifySlotStatusUpdateTopic")
                .build();

        openHourUpdateTopic.addSubscription(new SqsSubscription(openHourUpdateQueue));
        slotStatusUpdateTopic.addSubscription(new SqsSubscription(slotStatusUpdateQueue));
    }
}
