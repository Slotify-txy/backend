package org.slotify.infrastructure;

import software.amazon.awscdk.Fn;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.constructs.Construct;

public class SecurityGroupStack extends Stack {
    public SecurityGroupStack(final Construct scope,
                              final String id,
                              final StackProps props,
                              final ServiceInfo apiGatewayServiceInfo,
                              final ServiceInfo authServiceInfo,
                              final ServiceInfo userServiceInfo,
                              final ServiceInfo slotServiceInfo,
                              final ServiceInfo openHourServiceInfo,
                              final ServiceInfo emailTokenServiceInfo,
                              final ServiceInfo notificationServiceInfo
    ) {
        super(scope, id, props);

        ISecurityGroup useDbSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + userServiceInfo.getDbName() + "-sg",
                Fn.importValue(userServiceInfo.getDbName() + "-sg-id")
        );

        ISecurityGroup slotDbSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + slotServiceInfo.getDbName() + "-sg",
                Fn.importValue(slotServiceInfo.getDbName() + "-sg-id")
        );

        ISecurityGroup openHourDbSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + openHourServiceInfo.getDbName() + "-sg",
                Fn.importValue(openHourServiceInfo.getDbName() + "-sg-id")
        );

        ISecurityGroup emailTokenDbSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + emailTokenServiceInfo.getDbName() + "-sg",
                Fn.importValue(emailTokenServiceInfo.getDbName() + "-sg-id")
        );

        ISecurityGroup apiGatewaySG = SecurityGroup.fromSecurityGroupId(
            this,
            "imported-" + apiGatewayServiceInfo.getServiceName() + "-sg",
            Fn.importValue(apiGatewayServiceInfo.getServiceName() + "-sg-id")
        );

        ISecurityGroup authServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + authServiceInfo.getServiceName() + "-sg",
                Fn.importValue(authServiceInfo.getServiceName() + "-sg-id")
        );

        ISecurityGroup userServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + userServiceInfo.getServiceName() + "-sg",
                Fn.importValue(userServiceInfo.getServiceName() + "-sg-id")
        );

        ISecurityGroup slotServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + slotServiceInfo.getServiceName() + "-sg",
                Fn.importValue(slotServiceInfo.getServiceName() + "-sg-id")
        );

        ISecurityGroup openHourServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + openHourServiceInfo.getServiceName() + "-sg",
                Fn.importValue(openHourServiceInfo.getServiceName() + "-sg-id")
        );

        ISecurityGroup emailTokenServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + emailTokenServiceInfo.getServiceName() + "-sg",
                Fn.importValue(emailTokenServiceInfo.getServiceName() + "-sg-id")
        );


        ISecurityGroup notificationServiceSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + notificationServiceInfo.getServiceName() + "-sg",
                Fn.importValue(notificationServiceInfo.getServiceName() + "-sg-id")
        );

        authServiceSG.addIngressRule(apiGatewaySG, Port.tcp(authServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + authServiceInfo.getServiceName());
        userServiceSG.addIngressRule(apiGatewaySG, Port.tcp(userServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName());
        slotServiceSG.addIngressRule(apiGatewaySG, Port.tcp(slotServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + slotServiceInfo.getServiceName());
        openHourServiceSG.addIngressRule(apiGatewaySG, Port.tcp(openHourServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + openHourServiceInfo.getServiceName());
        emailTokenServiceSG.addIngressRule(apiGatewaySG, Port.tcp(emailTokenServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName());
        notificationServiceSG.addIngressRule(apiGatewaySG, Port.tcp(notificationServiceInfo.getServicePort()), "Allow " + apiGatewayServiceInfo.getServiceName() + " to connect to " + notificationServiceInfo.getServiceName());

        useDbSG.addIngressRule(userServiceSG, Port.tcp(3306), "Allow " + userServiceInfo.getServiceName() + " to access " + userServiceInfo.getDbName());
        slotDbSG.addIngressRule(userServiceSG, Port.tcp(3306), "Allow " + slotServiceInfo.getServiceName() + " to access " + slotServiceInfo.getDbName());
        openHourDbSG.addIngressRule(openHourServiceSG, Port.tcp(3306), "Allow " + openHourServiceInfo.getServiceName() + " to access " + openHourServiceInfo.getDbName());
        emailTokenDbSG.addIngressRule(emailTokenDbSG, Port.tcp(3306), "Allow " + emailTokenServiceInfo.getServiceName() + " to access " + emailTokenServiceInfo.getDbName());

        userServiceSG.addIngressRule(authServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + authServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        userServiceSG.addIngressRule(slotServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + slotServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        userServiceSG.addIngressRule(openHourServiceSG, Port.tcp(userServiceInfo.getGRPCPort()), "Allow " + openHourServiceInfo.getServiceName() + " to connect to " + userServiceInfo.getServiceName() + " gRPC endpoint");
        slotServiceSG.addIngressRule(userServiceSG, Port.tcp(slotServiceInfo.getGRPCPort()), "Allow " + userServiceInfo.getServiceName() + " to connect to " + slotServiceInfo.getServiceName() + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(slotServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + slotServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName() + " gRPC endpoint");
        emailTokenServiceSG.addIngressRule(notificationServiceSG, Port.tcp(emailTokenServiceInfo.getGRPCPort()), "Allow " + notificationServiceInfo.getServiceName() + " to connect to " + emailTokenServiceInfo.getServiceName() + " gRPC endpoint");

    }
}
