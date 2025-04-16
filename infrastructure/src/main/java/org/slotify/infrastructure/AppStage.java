package org.slotify.infrastructure;

import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Stage;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class AppStage extends Stage {
    public AppStage(final Construct scope, final String id, final StageProps props) {
        super(scope, id, props);
        ServiceInfo apiGatewayServiceInfo = new ServiceInfo("api-gateway", 8084, null, null);
        ServiceInfo authServiceInfo = new ServiceInfo("auth-service", 8082, null, null);
        ServiceInfo userServiceInfo = new ServiceInfo("user-service", 8081, 9001, "userDB");
        ServiceInfo slotServiceInfo = new ServiceInfo("slot-service", 8080, 9002, "slotDB");
        ServiceInfo openHourServiceInfo = new ServiceInfo("open-hour-service", 8083, null, "openHourDB");
        ServiceInfo emailTokenServiceInfo = new ServiceInfo("email-token-service", 8086, 9003, "emailTokenDB");
        ServiceInfo notificationServiceInfo = new ServiceInfo("notification-service", 8085, null, null);

        StackProps stackProps = StackProps.builder()
                .env(props.getEnv())
                .build();

        NetworkStack networkStack = new NetworkStack(this, "network-stack", stackProps);
        Vpc vpc = networkStack.getVpc();

        ClusterStack clusterStack = new ClusterStack(this, "cluster-stack", stackProps, vpc);
        Cluster ecsCluster = clusterStack.getEcsCluster();

        SNSAndSQSStack snsAndSQSStack = new SNSAndSQSStack(this, "sns-and-sqs-stack", stackProps);

        new SecurityGroupStack(this, "security-group-stack", stackProps, vpc, apiGatewayServiceInfo, authServiceInfo, userServiceInfo, slotServiceInfo, openHourServiceInfo, emailTokenServiceInfo, notificationServiceInfo);

        DatabaseStack useDB = new DatabaseStack(this, "slotify-" + userServiceInfo.getDbName(), stackProps, vpc, userServiceInfo.getDbName());
        DatabaseStack slotDB = new DatabaseStack(this, "slotify-" + slotServiceInfo.getDbName(), stackProps, vpc, slotServiceInfo.getDbName());
        DatabaseStack openHourDB = new DatabaseStack(this, "slotify-" + openHourServiceInfo.getDbName(), stackProps, vpc, openHourServiceInfo.getDbName());
        DatabaseStack emailTokenDB = new DatabaseStack(this, "slotify-" + emailTokenServiceInfo.getDbName(), stackProps, vpc, emailTokenServiceInfo.getDbName());

        ApiGatewayServiceStack apiGatewayServiceStack = new ApiGatewayServiceStack(
                this,
                "slotify-api-gateway-service",
                stackProps,
                ecsCluster,
                apiGatewayServiceInfo,
                apiGatewayServiceInfo.getServicePort(),
                "http://" + authServiceInfo.getServiceAddress() + ":" + authServiceInfo.getServicePort()
        );

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

        ServiceStack userServiceStack = new ServiceStack(
                this,
                "slotify-user-service",
                stackProps,
                ecsCluster,
                List.of(userServiceInfo.getServicePort(), userServiceInfo.getGRPCPort()),
                useDB.getDb(),
                userServiceInfo,
                Map.of(
                        "SLOT_SERVICE_ADDRESS", slotServiceInfo.getServiceAddress(),
                        "SLOT_SERVICE_GRPC_PORT", slotServiceInfo.getGRPCPort().toString()
                )
        );
//        FargateService userService = userServiceStack.getService();
//        userService.getNode().addDependency(databaseStack.getUserDbHealthCheck());
//        userService.getNode().addDependency(databaseStack.getUserDb());

        ServiceStack slotServiceStack = new ServiceStack(
                this,
                "slotify-slot-service",
                stackProps,
                ecsCluster,
                List.of(slotServiceInfo.getServicePort(), slotServiceInfo.getGRPCPort()),
                slotDB.getDb(),
                slotServiceInfo,
                Map.of(
                        "EMAIL_TOKEN_SERVICE_ADDRESS", emailTokenServiceInfo.getServiceAddress(),
                        "EMAIL_TOKEN_SERVICE_GRPC_PORT", emailTokenServiceInfo.getGRPCPort().toString(),
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", snsAndSQSStack.getSlotStatusUpdateTopic().getTopicArn()
                )
        );
//        FargateService slotService = slotServiceStack.getService();
//        slotService.getNode().addDependency(databaseStack.getSlotDbHealthCheck());
//        slotService.getNode().addDependency(databaseStack.getSlotDb());
//        slotService.getNode().addDependency(snsAndSQSStack.getSlotStatusUpdateTopic());

        ServiceStack openHourServiceStack = new ServiceStack(
                this,
                "slotify-open-hour-service",
                stackProps,
                ecsCluster,
                List.of(openHourServiceInfo.getServicePort()),
                openHourDB.getDb(),
                openHourServiceInfo,
                Map.of(
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", snsAndSQSStack.getOpenHourUpdateTopic().getTopicArn()
                )
        );
//        FargateService openHourService = openHourServiceStack.getService();
//        openHourService.getNode().addDependency(databaseStack.getOpenHourDbHealthCheck());
//        openHourService.getNode().addDependency(databaseStack.getOpenHourDb());
//        openHourService.getNode().addDependency(snsAndSQSStack.getSlotStatusUpdateTopic());

        ServiceStack emailTokenServiceStack = new ServiceStack(
                this,
                "slotify-email-token-service",
                stackProps,
                ecsCluster,
                List.of(emailTokenServiceInfo.getServicePort()),
                emailTokenDB.getDb(),
                emailTokenServiceInfo,
                null
        );
//        FargateService emailTokenService = emailTokenServiceStack.getService();
//        emailTokenService.getNode().addDependency(databaseStack.getEmailTokenDbHealthCheck());
//        emailTokenService.getNode().addDependency(databaseStack.getEmailTokenDb());

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
   }
}
