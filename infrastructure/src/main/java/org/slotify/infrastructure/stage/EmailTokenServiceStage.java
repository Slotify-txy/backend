package org.slotify.infrastructure.stage;

import org.slotify.infrastructure.ServiceStack;
import software.amazon.awscdk.StageProps;
import software.constructs.Construct;

import java.util.List;

public class EmailTokenServiceStage extends InfraStage{

    public EmailTokenServiceStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);

        ServiceStack emailTokenServiceStack = new ServiceStack(
                this,
                "slotify-email-token-service",
                stackProps,
                ecsCluster,
                List.of(emailTokenServiceInfo.getServicePort()),
                emailTokenDbStack.getDb(),
                emailTokenServiceInfo,
                null
        );

        emailTokenServiceStack.getNode().addDependency(securityGroupStack);
        emailTokenServiceStack.getNode().addDependency(emailTokenDbStack);
    }
}
