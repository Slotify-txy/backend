package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class UserServiceStage extends InfraStage{

    public UserServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack userServiceStack = new ServiceStack(
                this,
                "slotify-user-service",
                stackProps,
                ecsCluster,
                List.of(userServiceInfo.getServicePort(), userServiceInfo.getGRPCPort()),
                userDbStack.getDb(),
                userServiceInfo,
                Map.of(
                        "SLOT_SERVICE_ADDRESS", slotServiceInfo.getServiceAddress(),
                        "SLOT_SERVICE_GRPC_PORT", slotServiceInfo.getGRPCPort().toString()
                )
        );

        userServiceStack.getNode().addDependency(securityGroupStack);
        userServiceStack.getNode().addDependency(userDbStack);
    }
}
