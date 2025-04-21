package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ApiGatewayServiceStack;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

public class ApiGatewayServiceStage extends InfraStage{

    public ApiGatewayServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ApiGatewayServiceStack apiGatewayServiceStack = new ApiGatewayServiceStack(
                this,
                "slotify-api-gateway-service",
                stackProps,
                ecsCluster,
                apiGatewayServiceInfo,
                apiGatewayServiceInfo.getServicePort(),
                "http://" + authServiceInfo.getServiceAddress() + ":" + authServiceInfo.getServicePort()
        );

        apiGatewayServiceStack.getNode().addDependency(securityGroupStack);
    }
}
