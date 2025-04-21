package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class AuthServiceStage extends InfraStage{

    public AuthServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack authServiceStack = new ServiceStack(
                this,
                "slotify-auth-service",
                stackProps,
                ecsCluster,
                List.of(authServiceInfo.getServicePort()),
                null,
                authServiceInfo,
                Map.of(
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString()
                )
        );

        authServiceStack.getNode().addDependency(securityGroupStack);
    }
}
