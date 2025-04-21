package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class OpenHourServiceStage extends InfraStage{

    public OpenHourServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack openHourServiceStack = new ServiceStack(
                this,
                "slotify-open-hour-service",
                stackProps,
                ecsCluster,
                List.of(openHourServiceInfo.getServicePort()),
                openHourDbStack.getDb(),
                openHourServiceInfo,
                Map.of(
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", snsAndSQSStack.getOpenHourUpdateTopic().getTopicArn()
                )
        );

        openHourServiceStack.getNode().addDependency(securityGroupStack);
        openHourServiceStack.getNode().addDependency(openHourDbStack);

        IRole openHourServiceTaskRole = openHourServiceStack.getService().getTaskDefinition().getTaskRole();
        openHourServiceTaskRole.addManagedPolicy(
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSNSFullAccess")
        );
    }
}
