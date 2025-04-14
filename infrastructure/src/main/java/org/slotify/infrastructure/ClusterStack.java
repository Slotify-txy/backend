package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClusterStack extends Stack {

    private final Cluster ecsCluster;

    public ClusterStack(final Construct scope,
                        final String id,
                        final StackProps props,
                        final Vpc vpc
    ) {
        super(scope, id, props);
        this.ecsCluster = createEcsCluster(vpc);
    }

    private Cluster createEcsCluster(Vpc vpc){
        return Cluster.Builder.create(this, "SlotifyCluster")
                .vpc(vpc)
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("slotify.local")
                        .build())
                .build();
    }
}
