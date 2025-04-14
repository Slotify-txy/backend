package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceStack extends Stack {

    private FargateService service;
    private SecurityGroup serviceSG;

    public ServiceStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final Vpc vpc,
            final Cluster ecsCluster,
            final List<Integer> ports,
            final DatabaseInstance db,
            final ServiceInfo serviceInfo,
            final Map<String, String> additionalEnvVars
    ) {
        super(scope, id, props);
        this.serviceSG = createSG(vpc, serviceInfo.getServiceName());
        this.service = createFargateService(
                ecsCluster,
                serviceInfo.getServiceName(),
                ports,
                db,
                serviceInfo.getDbName(),
                serviceSG,
                additionalEnvVars
        );

        CfnOutput.Builder.create(this, serviceInfo.getServiceName() + "-sg-export")
                .value(serviceSG.getSecurityGroupId())
                .exportName(serviceInfo.getServiceName() + "-sg-id")
                .build();
    }


    FargateService createFargateService(
            Cluster ecsCluster,
            String imageName,
            List<Integer> ports,
            DatabaseInstance db,
            String dbName,
            SecurityGroup serviceSG,
            Map<String, String> additionalEnvVars
    ) {


        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, imageName + "Task")
                        .cpu(1024)
                        .memoryLimitMiB(2048)
                        .build();

        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromAsset("../" + imageName))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, imageName + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()));

        Map<String, String> envVars = new HashMap<>();

        if(additionalEnvVars != null){
            envVars.putAll(additionalEnvVars);
        }

        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:mysql://%s:%s/%s".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    dbName
            ));
            envVars.put("SPRING_DATASOURCE_USERNAME", "slotify");
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    Objects.requireNonNull(db.getSecret()).secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, imageName)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .securityGroups(List.of(serviceSG))
                .serviceName(imageName)
                .cloudMapOptions(CloudMapOptions.builder()
                        .name(imageName)
                        .build())
                .build();
    }

    private SecurityGroup createSG(Vpc vpc, String name) {
        return SecurityGroup.Builder.create(this,"slotify_" + name + "_SG")
                .vpc(vpc)
                .description("Security group for " + name)
                .allowAllOutbound(true)
                .build();
    }
}
