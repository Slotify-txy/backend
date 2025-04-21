package org.slotify.infrastructure.stage;

import lombok.Getter;
import org.slotify.infrastructure.*;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

@Getter
public class InfraStage extends BaseStage{
    final StackProps stackProps;
    final SecurityGroupStack securityGroupStack;
    final Cluster ecsCluster;
    final SNSAndSQSStack snsAndSQSStack;
    final DatabaseStack userDbStack;
    final DatabaseStack slotDbStack;
    final DatabaseStack openHourDbStack;
    final DatabaseStack emailTokenDbStack;

    public InfraStage(Construct scope, String id, StageProps props) {
        super(scope, id, props);
        this.stackProps = StackProps.builder()
                .env(props.getEnv())
                .build();

        NetworkStack networkStack = new NetworkStack(this, "network", stackProps);
        Vpc vpc = networkStack.getVpc();

        ClusterStack clusterStack = new ClusterStack(this, "cluster", stackProps, vpc);
        this.ecsCluster = clusterStack.getEcsCluster();
        this.snsAndSQSStack = new SNSAndSQSStack(this, "sns-and-sqs", stackProps);
        this.securityGroupStack = new SecurityGroupStack(this, "security-group", stackProps, vpc, apiGatewayServiceInfo, authServiceInfo, userServiceInfo, slotServiceInfo, openHourServiceInfo, emailTokenServiceInfo, notificationServiceInfo);

        this.userDbStack = new DatabaseStack(this, "slotify-" + userServiceInfo.getDbName(), stackProps, vpc, userServiceInfo.getDbName());
        this.slotDbStack = new DatabaseStack(this, "slotify-" + slotServiceInfo.getDbName(), stackProps, vpc, slotServiceInfo.getDbName());
        this.openHourDbStack = new DatabaseStack(this, "slotify-" + openHourServiceInfo.getDbName(), stackProps, vpc, openHourServiceInfo.getDbName());
        this.emailTokenDbStack = new DatabaseStack(this, "slotify-" + emailTokenServiceInfo.getDbName(), stackProps, vpc, emailTokenServiceInfo.getDbName());

        userDbStack.getNode().addDependency(securityGroupStack);
        slotDbStack.getNode().addDependency(securityGroupStack);
        openHourDbStack.getNode().addDependency(securityGroupStack);
        emailTokenDbStack.getNode().addDependency(securityGroupStack);
    }
}
