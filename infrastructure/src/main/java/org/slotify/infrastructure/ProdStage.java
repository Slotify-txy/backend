package org.slotify.infrastructure;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ProdStage extends Stage {
    public ProdStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);
        ServiceInfo apiGatewayServiceInfo = new ServiceInfo("api-gateway", 8084, null, null);
        ServiceInfo authServiceInfo = new ServiceInfo("auth-service", 8082, null, null);
        ServiceInfo userServiceInfo = new ServiceInfo("user-service", 8081, 9001, "userDB");
        ServiceInfo slotServiceInfo = new ServiceInfo("slot-service", 8080, 9002, "slotDb");
        ServiceInfo openHourServiceInfo = new ServiceInfo("open-hour-service", 8083, null, "openHourDb");
        ServiceInfo emailTokenServiceInfo = new ServiceInfo("email-token-service", 8086, 9003, "emailTokenDb");
        ServiceInfo notificationServiceInfo = new ServiceInfo("notification-service", 8085, null, null);

        StackProps stackProps = StackProps.builder()
                .env(props.getEnv())
                .build();

        NetworkStack networkStack = new NetworkStack(this, "network-stack", stackProps);
        Vpc vpc = networkStack.getVpc();

        ClusterStack clusterStack = new ClusterStack(this, "cluster-stack", stackProps, vpc);
        Cluster ecsCluster = clusterStack.getEcsCluster();

        SNSAndSQSStack snsAndSQSStack = new SNSAndSQSStack(this, "sns-and-sqs-stack", stackProps);

        SecurityGroupStack securityGroupStack = new SecurityGroupStack(this, "security-group-stack", stackProps, vpc, apiGatewayServiceInfo, authServiceInfo, userServiceInfo, slotServiceInfo, openHourServiceInfo, emailTokenServiceInfo, notificationServiceInfo);

        DatabaseStack userDbStack = new DatabaseStack(this, "slotify-" + userServiceInfo.getDbName() + "-stack", stackProps, vpc, userServiceInfo.getDbName());
        DatabaseStack slotDbStack = new DatabaseStack(this, "slotify-" + slotServiceInfo.getDbName() + "-stack", stackProps, vpc, slotServiceInfo.getDbName());
        DatabaseStack openHourDbStack = new DatabaseStack(this, "slotify-" + openHourServiceInfo.getDbName() + "-stack", stackProps, vpc, openHourServiceInfo.getDbName());
        DatabaseStack emailTokenDbStack = new DatabaseStack(this, "slotify-" + emailTokenServiceInfo.getDbName() + "-stack", stackProps, vpc, emailTokenServiceInfo.getDbName());

        userDbStack.getNode().addDependency(securityGroupStack);
        slotDbStack.getNode().addDependency(securityGroupStack);
        openHourDbStack.getNode().addDependency(securityGroupStack);
        emailTokenDbStack.getNode().addDependency(securityGroupStack);

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
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        );

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
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSQSFullAccess")
        );

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
                ManagedPolicy.fromAwsManagedPolicyName("AmazonSNSFullAccess")
        );
    }
}
