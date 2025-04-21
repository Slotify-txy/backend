package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class SlotServiceStage extends InfraStage{

    public SlotServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack slotServiceStack = new ServiceStack(
                this,
                "slotify-slot-service",
                stackProps,
                ecsCluster,
                List.of(slotServiceInfo.getServicePort(), slotServiceInfo.getGRPCPort()),
                slotDbStack.getDb(),
                slotServiceInfo,
                Map.of(
                        "EMAIL_TOKEN_SERVICE_ADDRESS", emailTokenServiceInfo.getServiceAddress(),
                        "EMAIL_TOKEN_SERVICE_GRPC_PORT", emailTokenServiceInfo.getGRPCPort().toString(),
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", snsAndSQSStack.getSlotStatusUpdateTopic().getTopicArn()
                )
        );

        slotServiceStack.getNode().addDependency(securityGroupStack);
        slotServiceStack.getNode().addDependency(slotDbStack);

        IRole slotServiceTaskRole = slotServiceStack.getService().getTaskDefinition().getTaskRole();
        slotServiceTaskRole.addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSNSFullAccess")
        );
    }
}
