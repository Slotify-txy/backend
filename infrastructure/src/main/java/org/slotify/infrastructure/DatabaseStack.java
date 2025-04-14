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
    private final SecurityGroup dbSG;

    public DatabaseStack(
            final Construct scope,
            final String id,
            final StackProps props,
            final Vpc vpc,
            final String dbName
    ) {
        super(scope, id, props);
        this.dbSG = createSG(vpc, dbName);

        CfnOutput.Builder.create(this, dbName + "-sg-export")
                .value(dbSG.getSecurityGroupId())
                .exportName(dbName + "-sg-id")
                .build();

        this.db = createDatabase(vpc, dbName, dbName, dbSG);
        this.healthCheck =
                createDbHealthCheck(db, dbName + "HealthCheck");
    }

    private DatabaseInstance createDatabase(Vpc vpc, String id, String dbName, SecurityGroup sg) {
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

    private SecurityGroup createSG(Vpc vpc, String name) {
        return SecurityGroup.Builder.create(this,"slotify_" + name + "_SG")
                .vpc(vpc)
                .description("Security group for " + name)
                .allowAllOutbound(true)
                .build();
    }
}
