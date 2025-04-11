package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatabaseStack extends Stack {
    private final Vpc vpc;
    private final DatabaseInstance userDb;
    private final DatabaseInstance slotDb;
    private final DatabaseInstance openHourDb;
    private final DatabaseInstance emailTokenDb;
    private final CfnHealthCheck userDbHealthCheck;
    private final CfnHealthCheck slotDbHealthCheck;
    private final CfnHealthCheck openHourDbHealthCheck;
    private final CfnHealthCheck emailTokenDbHealthCheck;

    public DatabaseStack(
            final App scope,
            final String id,
            final StackProps props,
            final Vpc vpc,
            final String userDbName,
            final String slotDbName,
            final String openHourDbName,
            final String emailTokenDbName,
            final SecurityGroup userDbSG,
            final SecurityGroup slotDbSG,
            final SecurityGroup openHourDbSG,
            final SecurityGroup emailTokenDbSG
    ) {
        super(scope, id, props);
        this.vpc = vpc;

        this.userDb = createDatabase(userDbName, userDbName, userDbSG);
        this.slotDb =
                createDatabase(slotDbName, slotDbName, slotDbSG);
        this.openHourDb =
                createDatabase(openHourDbName, openHourDbName, openHourDbSG);
        this.emailTokenDb =
                createDatabase(emailTokenDbName, emailTokenDbName, emailTokenDbSG);

        this.userDbHealthCheck =
                createDbHealthCheck(userDb, userDbName + "HealthCheck");

        this.slotDbHealthCheck =
                createDbHealthCheck(slotDb, slotDbName + "HealthCheck");

        this.openHourDbHealthCheck =
                createDbHealthCheck(openHourDb, openHourDbName + "HealthCheck");

        this.emailTokenDbHealthCheck =
                createDbHealthCheck(emailTokenDb, emailTokenDbName + "HealthCheck");
    }

    private DatabaseInstance createDatabase(String id, String dbName, SecurityGroup sg) {
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
