package org.slotify.infrastructure;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

@EqualsAndHashCode(callSuper = true)
@Data
public class NetworkStack extends Stack {
    private final Vpc vpc;

    public NetworkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();
    }

    private Vpc createVpc(){
        return Vpc.Builder
                .create(this, "SlotifyVPC")
                .vpcName("SlotifyVPC")
                .maxAzs(2)
                .build();
    }
}
