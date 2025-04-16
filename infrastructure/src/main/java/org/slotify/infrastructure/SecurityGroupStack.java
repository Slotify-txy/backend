package org.slotify.infrastructure;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

public class SecurityGroupStack extends Stack {
    public SecurityGroupStack(final Construct scope,
                              final String id,
                              final StackProps props,
                              final Vpc vpc,
                              final ServiceInfo apiGatewayServiceInfo,
                              final ServiceInfo authServiceInfo,
                              final ServiceInfo userServiceInfo,
                              final ServiceInfo slotServiceInfo,
                              final ServiceInfo openHourServiceInfo,
                              final ServiceInfo emailTokenServiceInfo,
                              final ServiceInfo notificationServiceInfo
    ) {
        super(scope, id, props);

        String userDbName = userServiceInfo.getDbName();
        SecurityGroup useDbSG = createSG(vpc, userDbName);

        String slotDbName = slotServiceInfo.getDbName();
        SecurityGroup slotDbSG = createSG(vpc, slotDbName);

        String openHourDbName = openHourServiceInfo.getDbName();
        SecurityGroup openHourDbSG = createSG(vpc, openHourDbName);

        String emailTokenDbName = emailTokenServiceInfo.getDbName();
        SecurityGroup emailTokenDbSG = createSG(vpc, emailTokenDbName);

        String apiGatewayServiceName = apiGatewayServiceInfo.getServiceName();
        SecurityGroup apiGatewaySG = createSG(vpc, apiGatewayServiceName);

        String authServiceName = authServiceInfo.getServiceName();
        SecurityGroup authServiceSG = createSG(vpc, authServiceName);

        String userServiceName = userServiceInfo.getServiceName();
        SecurityGroup userServiceSG = createSG(vpc, userServiceName);

        String slotServiceName = slotServiceInfo.getServiceName();
        SecurityGroup slotServiceSG = createSG(vpc, slotServiceName);

        String openHourServiceName = openHourServiceInfo.getServiceName();
        SecurityGroup openHourServiceSG = createSG(vpc, openHourServiceName);

        String emailTokenServiceName = emailTokenServiceInfo.getServiceName();
        SecurityGroup emailTokenServiceSG = createSG(vpc, emailTokenServiceName);

        String notificationServiceName = notificationServiceInfo.getServiceName();
        SecurityGroup notificationServiceSG = createSG(vpc, notificationServiceName);

        authServiceSG.addIngressRule(apiGatewaySG, Port.tcp(authServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + authServiceName);
        userServiceSG.addIngressRule(apiGatewaySG, Port.tcp(userServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + userServiceName);
        slotServiceSG.addIngressRule(apiGatewaySG, Port.tcp(slotServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + slotServiceName);
        openHourServiceSG.addIngressRule(apiGatewaySG, Port.tcp(openHourServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + openHourServiceName);
        emailTokenServiceSG.addIngressRule(apiGatewaySG, Port.tcp(emailTokenServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + emailTokenServiceName);
        notificationServiceSG.addIngressRule(apiGatewaySG, Port.tcp(notificationServiceInfo.getServicePort()), "Allow " + apiGatewayServiceName + " to connect to " + notificationServiceName);

        useDbSG.addIngressRule(userServiceSG, Port.tcp(3306), "Allow " + userServiceName + " to access " + userDbName);
        slotDbSG.addIngressRule(slotServiceSG, Port.tcp(3306), "Allow " + slotServiceName + " to access " + slotDbName);
        openHourDbSG.addIngressRule(openHourServiceSG, Port.tcp(3306), "Allow " + openHourServiceName + " to access " + openHourDbName);
        emailTokenDbSG.addIngressRule(emailTokenServiceSG, Port.tcp(3306), "Allow " + emailTokenServiceName + " to access " + emailTokenDbName);

        userServiceSG.addIngressRule(authServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + authServiceName + " to connect to " + userServiceName + " gRPC endpoint");
        userServiceSG.addIngressRule(slotServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + slotServiceName + " to connect to " + userServiceName + " gRPC endpoint");
        userServiceSG.addIngressRule(openHourServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + openHourServiceName + " to connect to " + userServiceName + " gRPC endpoint");
        slotServiceSG.addIngressRule(userServiceSG, Port.tcp(slotServiceInfo.getGRPCPort()), "Allow " + userServiceName + " to connect to " + slotServiceName + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(slotServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + slotServiceName + " to connect to " + emailTokenServiceName + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(notificationServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + notificationServiceName + " to connect to " + emailTokenServiceName + " gRPC endpoint");

        exportSG(userDbName, useDbSG);
        exportSG(slotDbName, slotDbSG);
        exportSG(openHourDbName, openHourDbSG);
        exportSG(emailTokenDbName, emailTokenDbSG);
        exportSG(apiGatewayServiceName, apiGatewaySG);
        exportSG(authServiceName, authServiceSG);
        exportSG(userServiceName, userServiceSG);
        exportSG(slotServiceName, slotServiceSG);
        exportSG(openHourServiceName, openHourServiceSG);
        exportSG(emailTokenServiceName, emailTokenServiceSG);
        exportSG(notificationServiceName, notificationServiceSG);
    }

    private SecurityGroup createSG(Vpc vpc, String name) {
        return SecurityGroup.Builder.create(this,"slotify_" + name + "_SG")
                .vpc(vpc)
                .description("Security group for " + name)
                .allowAllOutbound(true)
                .build();
    }

    private void exportSG(String name, SecurityGroup securityGroup) {
        CfnOutput.Builder.create(this, name + "-sg-export")
                .value(securityGroup.getSecurityGroupId())
                .exportName(name + "-sg-id")
                .build();
    }
}
