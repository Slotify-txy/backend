package org.slotify.infrastructure;

import java.util.List;
import java.util.Map;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;

public class InfrastructureApplication extends Stack {
    public static void main(final String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        StackProps props = StackProps
                            .builder()
                            .env(Environment.builder()
                                    .account("697816050693")
                                    .region("us-east-1")
                                    .build())
                            .build();

        ServiceInfo apiGatewayServiceInfo = new ServiceInfo("api-gateway", 8084, null, null);
        ServiceInfo authServiceInfo = new ServiceInfo("auth-service", 8082, null, null);
        ServiceInfo userServiceInfo = new ServiceInfo("user-service", 8081, 9001, "userDB");
        ServiceInfo slotServiceInfo = new ServiceInfo("slot-service", 8080, 9002, "slotDB");
        ServiceInfo openHourServiceInfo = new ServiceInfo("open-hour-service", 8083, null, "openHourDB");
        ServiceInfo emailTokenServiceInfo = new ServiceInfo("email-token-service", 8086, 9003, "emailTokenDB");
        ServiceInfo notificationServiceInfo = new ServiceInfo("notification-service", 8085, null, null);

        CoreStack coreStack = new CoreStack(
                app,
                "slotify-core",
                props,
                apiGatewayServiceInfo,
                authServiceInfo,
                userServiceInfo,
                slotServiceInfo,
                openHourServiceInfo,
                emailTokenServiceInfo,
                notificationServiceInfo
        );
        Vpc vpc = coreStack.getVpc();
        Cluster ecsCluster = coreStack.getEcsCluster();

        DatabaseStack databaseStack = new DatabaseStack(
                app,
                "slotify-databases",
                props,
                vpc,
                userServiceInfo.getDbName(),
                slotServiceInfo.getDbName(),
                openHourServiceInfo.getDbName(),
                emailTokenServiceInfo.getDbName(),
                coreStack.getUserDbSG(),
                coreStack.getSlotDbSG(),
                coreStack.getOpenHourDbSG(),
                coreStack.getEmailTokenDbSG()
        );

        ServiceStack authServiceStack = new ServiceStack(
                app,
                "slotify-auth-service",
                props,
                ecsCluster,
                authServiceInfo.getServiceName(),
                List.of(authServiceInfo.getServicePort()),
                null,
                authServiceInfo.getDbName(),
                null,
                coreStack.getAuthServiceSG(),
                Map.of(
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString()
                )
        );

        ServiceStack userServiceStack = new ServiceStack(
                app,
                "slotify-user-service",
                props,
                ecsCluster,
                userServiceInfo.getServiceName(),
                List.of(userServiceInfo.getServicePort(), userServiceInfo.getGRPCPort()),
                databaseStack.getUserDb(),
                userServiceInfo.getDbName(),
                coreStack.getUserDbSG(),
                coreStack.getUserServiceSG(),
                Map.of(
                        "SLOT_SERVICE_ADDRESS", slotServiceInfo.getServiceAddress(),
                        "SLOT_SERVICE_GRPC_PORT", slotServiceInfo.getGRPCPort().toString()
                )
        );
        FargateService userService = userServiceStack.getService();
        userService.getNode().addDependency(databaseStack.getUserDbHealthCheck());
        userService.getNode().addDependency(databaseStack.getUserDb());

        ServiceStack slotServiceStack = new ServiceStack(
                app,
                "slotify-slot-service",
                props,
                ecsCluster,
                slotServiceInfo.getServiceName(),
                List.of(slotServiceInfo.getServicePort(), slotServiceInfo.getGRPCPort()),
                databaseStack.getSlotDb(),
                slotServiceInfo.getDbName(),
                coreStack.getSlotDbSG(),
                coreStack.getSlotServiceSG(),
                Map.of(
                        "EMAIL_TOKEN_SERVICE_ADDRESS", emailTokenServiceInfo.getServiceAddress(),
                        "EMAIL_TOKEN_SERVICE_GRPC_PORT", emailTokenServiceInfo.getGRPCPort().toString(),
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", coreStack.getSlotStatusUpdateTopic().getTopicArn()
                )
        );
        FargateService slotService = slotServiceStack.getService();
        slotService.getNode().addDependency(databaseStack.getSlotDbHealthCheck());
        slotService.getNode().addDependency(databaseStack.getSlotDb());
        slotService.getNode().addDependency(coreStack.getSlotStatusUpdateTopic());

        ServiceStack openHourServiceStack = new ServiceStack(
                app,
                "slotify-open-hour-service",
                props,
                ecsCluster,
                openHourServiceInfo.getServiceName(),
                List.of(openHourServiceInfo.getServicePort()),
                databaseStack.getOpenHourDb(),
                openHourServiceInfo.getDbName(),
                coreStack.getOpenHourDbSG(),
                coreStack.getOpenHourServiceSG(),
                Map.of(
                        "USER_SERVICE_ADDRESS", userServiceInfo.getServiceAddress(),
                        "USER_SERVICE_GRPC_PORT", userServiceInfo.getGRPCPort().toString(),
                        "SPRING_CLOUD_AWS_SNS_TOPIC_ARN", coreStack.getOpenHourUpdateTopic().getTopicArn()
                )
        );
        FargateService openHourService = openHourServiceStack.getService();
        openHourService.getNode().addDependency(databaseStack.getOpenHourDbHealthCheck());
        openHourService.getNode().addDependency(databaseStack.getOpenHourDb());
        openHourService.getNode().addDependency(coreStack.getSlotStatusUpdateTopic());

        ServiceStack emailTokenServiceStack = new ServiceStack(
                app,
                "slotify-email-token-service",
                props,
                ecsCluster,
                emailTokenServiceInfo.getServiceName(),
                List.of(emailTokenServiceInfo.getServicePort()),
                databaseStack.getEmailTokenDb(),
                emailTokenServiceInfo.getDbName(),
                coreStack.getEmailTokenDbSG(),
                coreStack.getEmailTokenServiceSG(),
                null
        );
        FargateService emailTokenService = emailTokenServiceStack.getService();
        emailTokenService.getNode().addDependency(databaseStack.getEmailTokenDbHealthCheck());
        emailTokenService.getNode().addDependency(databaseStack.getEmailTokenDb());

        ServiceStack notificationServiceStack = new ServiceStack(
                app,
                "slotify-notification-service",
                props,
                ecsCluster,
                notificationServiceInfo.getServiceName(),
                List.of(notificationServiceInfo.getServicePort()),
                null,
                notificationServiceInfo.getDbName(),
                null,
                coreStack.getNotificationServiceSG(),
                Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                    "EMAIL_TOKEN_SERVICE_ADDRESS", emailTokenServiceInfo.getServiceAddress(),
                    "EMAIL_TOKEN_SERVICE_GRPC_PORT", emailTokenServiceInfo.getGRPCPort().toString(),
                    "SPRING_CLOUD_AWS_SQS_QUEUE_OPEN-HOUR-UPDATE", coreStack.getOpenHourUpdateQueue().getQueueName(),
                    "SPRING_CLOUD_AWS_SQS_QUEUE_SLOT-STATUS-UPDATE", coreStack.getSlotStatusUpdateQueue().getQueueName()
                )
        );

        app.synth();
        System.out.println("App synthesizing in progress...");
    }
}
