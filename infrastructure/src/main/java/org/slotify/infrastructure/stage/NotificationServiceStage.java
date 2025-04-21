package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class NotificationServiceStage extends InfraStage{

    public NotificationServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack notificationServiceStack = new ServiceStack(
                this,
                "slotify-notification-service",
                stackProps,
                ecsCluster,
                List.of(notificationServiceInfo.getServicePort()),
                null,
                notificationServiceInfo,
                Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "EMAIL_TOKEN_SERVICE_ADDRESS", emailTokenServiceInfo.getServiceAddress(),
                        "EMAIL_TOKEN_SERVICE_GRPC_PORT", emailTokenServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SQS_QUEUE_OPEN-HOUR-UPDATE", snsAndSQSStack.getOpenHourUpdateQueue().getQueueName(),
                        "SPRING_CLOUD_AWS_SQS_QUEUE_SLOT-STATUS-UPDATE", snsAndSQSStack.getSlotStatusUpdateQueue().getQueueName()
                )
        );
        notificationServiceStack.getNode().addDependency(securityGroupStack);

        IRole notificationServiceTaskRole = notificationServiceStack.getService().getTaskDefinition().getTaskRole();
        notificationServiceTaskRole.addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        );
        notificationServiceTaskRole.addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSESFullAccess")
        );
    }
}
