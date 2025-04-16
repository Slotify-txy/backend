package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatabaseStack extends Stack {
    private final DatabaseInstance db;
    private final CfnHealthCheck healthCheck;

    public DatabaseStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final Vpc vpc,
            final String dbName
    ) {
        super(scope, id, props);

        ISecurityGroup dbSG = SecurityGroup.fromSecurityGroupId(
                this,
                "imported-" + dbName + "-sg",
                Fn.importValue(dbName + "-sg-id")
        );

        this.db = createDatabase(vpc, dbName, dbName, dbSG);
        this.healthCheck =
                createDbHealthCheck(db, dbName + "HealthCheck");
    }

    private DatabaseInstance createDatabase(Vpc vpc, String id, String dbName, ISecurityGroup sg) {
        return DatabaseInstance.Builder
                .create(this, id)
                .engine(DatabaseInstanceEngine.mysql(
                        MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_8_0)
                                .build()))
                .vpc(vpc)
                .securityGroups(List.of(sg))
                .instanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("slotify"))
                .databaseName(dbName)
                .parameterGroup(
                        ParameterGroup
                                .Builder
                                .create(this, dbName + "_parameter_group")
                                .engine(DatabaseInstanceEngine.mysql(
                                        MySqlInstanceEngineProps.builder()
                                                .version(MysqlEngineVersion.VER_8_0)
                                                .build()))
                                .parameters(Map.of("time_zone", "US/Pacific"))
                                .build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .fullyQualifiedDomainName(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }
}
