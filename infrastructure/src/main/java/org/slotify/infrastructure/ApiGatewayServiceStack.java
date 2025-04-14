package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApiGatewayServiceStack extends Stack {
    private final SecurityGroup serviceSG;
    public ApiGatewayServiceStack(
            final Construct scope,
            final Vpc vpc,
            final String id,
            final StackProps props,
            final Cluster ecsCluster,
            final ServiceInfo serviceInfo,
            final Integer servicePort,
            final String authServiceUrl
    ) {
        super(scope, id, props);
        this.serviceSG = createSG(vpc, serviceInfo.getServiceName());

        CfnOutput.Builder.create(this, serviceInfo.getServiceName() + "-sg-export")
                .value(serviceSG.getSecurityGroupId())
                .exportName(serviceInfo.getServiceName() + "-sg-id")
                .build();

        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

        ContainerDefinitionOptions containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromAsset("../api-gateway"))
                        .environment(Map.of(
                                "SPRING_PROFILES_ACTIVE", "prod",
                                "AUTH_SERVICE_URL", authServiceUrl
                        ))
                        .portMappings(Stream.of(servicePort)
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                        .logGroupName("/ecs/api-gateway")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix("api-gateway")
                                .build()))
                        .build();


        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        IHostedZone hostedZone = HostedZone.fromLookup(this, "slotify-backend.com",
                HostedZoneProviderProps.builder()
                        .domainName("slotify-backend.com")
                        .build());

        Certificate certificate = Certificate.Builder.create(this, "APIGatewayCertificate")
                .domainName("api.slotify-backend.com")
                .validation(CertificateValidation.fromDns(hostedZone))
                .build();


        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                        .cluster(ecsCluster)
                        .serviceName("api-gateway")
                        .taskDefinition(taskDefinition)
                        .desiredCount(1)
                        .healthCheckGracePeriod(Duration.seconds(60))
                        .securityGroups(List.of(serviceSG))
                        .listenerPort(443)
                        .certificate(certificate)
                        .build();

        apiGateway.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health")
                .port("8084")
                .healthyHttpCodes("200")
                .build());

        // Create an A record in Route 53 (alias) for api.slotify-backend.com pointing to the ALB.
        ARecord.Builder.create(this, "ApiAliasRecord")
                .zone(hostedZone)
                .recordName("api") // This creates: api.slotify-backend.com
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(apiGateway.getLoadBalancer())))
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
