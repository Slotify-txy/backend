package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = true)
@Data
public class CoreStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final SecurityGroup userDbSG;
    private final SecurityGroup slotDbSG;
    private final SecurityGroup openHourDbSG;
    private final SecurityGroup emailTokenDbSG;

    private final SecurityGroup apiGatewaySG;
    private final SecurityGroup authServiceSG;
    private final SecurityGroup userServiceSG;
    private final SecurityGroup slotServiceSG;
    private final SecurityGroup openHourServiceSG;
    private final SecurityGroup emailTokenServiceSG;
    private final SecurityGroup notificationServiceSG;

    private final Topic openHourUpdateTopic;
    private final Topic slotStatusUpdateTopic;
    private final Queue openHourUpdateQueue;
    private final Queue slotStatusUpdateQueue;

    public CoreStack(
            final App scope,
            final String id,
            final StackProps props,
            final ServiceInfo apiGatewayServiceInfo,
            final ServiceInfo authServiceInfo,
            final ServiceInfo userServiceInfo,
            final ServiceInfo slotServiceInfo,
            final ServiceInfo openHourServiceInfo,
            final ServiceInfo emailTokenServiceInfo,
            final ServiceInfo notificationServiceInfo
    ){
        super(scope, id, props);
        this.vpc = createVpc();
        this.ecsCluster = createEcsCluster();

        this.userDbSG = createSG(userServiceInfo.getDbName());
        this.slotDbSG = createSG(slotServiceInfo.getDbName());
        this.openHourDbSG = createSG(openHourServiceInfo.getDbName());
        this.emailTokenDbSG = createSG(emailTokenServiceInfo.getDbName());

        this.apiGatewaySG = createSG(apiGatewayServiceInfo.getServiceName());
        this.authServiceSG = createSG(authServiceInfo.getServiceName());
        this.userServiceSG = createSG(userServiceInfo.getServiceName());
        this.slotServiceSG = createSG(slotServiceInfo.getServiceName());
        this.openHourServiceSG = createSG(openHourServiceInfo.getServiceName());
        this.emailTokenServiceSG = createSG(emailTokenServiceInfo.getServiceName());
        this.notificationServiceSG = createSG(notificationServiceInfo.getServiceName());

        // allow api gateway to access services
        authServiceSG.addIngressRule(apiGatewaySG, Port.tcp(authServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + authServiceInfo.getServiceName());
        userServiceSG.addIngressRule(apiGatewaySG, Port.tcp(userServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName());
        slotServiceSG.addIngressRule(apiGatewaySG, Port.tcp(slotServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + slotServiceInfo.getServiceName());
        openHourServiceSG.addIngressRule(apiGatewaySG, Port.tcp(openHourServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + openHourServiceInfo.getServiceName());
        emailTokenServiceSG.addIngressRule(apiGatewaySG, Port.tcp(emailTokenServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName());
        notificationServiceSG.addIngressRule(apiGatewaySG, Port.tcp(notificationServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + notificationServiceInfo.getServiceName());

        // allow grpc communication
        userServiceSG.addIngressRule(authServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + authServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        userServiceSG.addIngressRule(slotServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + slotServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        userServiceSG.addIngressRule(openHourServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + openHourServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        slotServiceSG.addIngressRule(userServiceSG, Port.tcp(slotServiceInfo.getGRPCPort()), "Allow " + userServiceInfo.getServiceName() + " to connect to " + slotServiceInfo.getServiceName() + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(slotServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + slotServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName() + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(notificationServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + notificationServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName() + " gRPC endpoint");

        // create SNS & SQS
        this.openHourUpdateQueue = Queue.Builder.create(this, "SlotifyOpenHourUpdateQueue")
                .queueName("SlotifyOpenHourUpdateQueue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        this.slotStatusUpdateQueue = Queue.Builder.create(this, "SlotifySlotStatusUpdateQueue")
                .queueName("SlotifySlotStatusUpdateQueue")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        this.openHourUpdateTopic = Topic.Builder.create(this, "SlotifyOpenHourUpdateTopic")
                .topicName("SlotifyOpenHourUpdateTopic")
                .build();

        this.slotStatusUpdateTopic = Topic.Builder.create(this, "SlotifySlotStatusUpdateTopic")
                .topicName("SlotifySlotStatusUpdateTopic")
                .build();

        openHourUpdateTopic.addSubscription(new SqsSubscription(openHourUpdateQueue));
        slotStatusUpdateTopic.addSubscription(new SqsSubscription(slotStatusUpdateQueue));

        createApiGatewayService(apiGatewayServiceInfo.getServicePort(), apiGatewaySG, "http://" + authServiceInfo.getServiceAddress() + ":" + authServiceInfo.getServicePort());
    }


    private Vpc createVpc(){
        return Vpc.Builder
                .create(this, "SlotifyVPC")
                .vpcName("SlotifyVPC")
                .maxAzs(2)
                .build();
    }

    private Cluster createEcsCluster(){
        return Cluster.Builder.create(this, "SlotifyCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("slotify.local")
                        .build())
                .build();
    }

    private SecurityGroup createSG(String name) {
        return SecurityGroup.Builder.create(this,"slotify_" + name + "_SG")
                .vpc(vpc)
                .description("Security group for " + name)
                .allowAllOutbound(true)
                .build();
    }

    private void createApiGatewayService(Integer servicePort, SecurityGroup sg, String authServiceUrl) {
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
                        .securityGroups(List.of(sg))
                        .listenerPort(443)
                        .certificate(certificate)
                        .build();

        apiGateway.getTargetGroup().configureHealthCheck(software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck.builder()
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
}
