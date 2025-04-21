package org.slotify.infrastructure;

import org.slotify.infrastructure.stage.*;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.*;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;

public class PipelineStack extends Stack {
    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        CodePipelineSource source = CodePipelineSource.connection(
                "Slotify-txy/backend",
                "master",
                ConnectionSourceOptions.builder()
                        .connectionArn("arn:aws:codeconnections:us-east-1:697816050693:connection/29814782-c70b-4fd3-9a66-0527ba9a2a48")
                        .build()
        );

        CodePipeline pipeline = CodePipeline.Builder.create(this, "SlotifyPipeline")
                .pipelineName("SlotifyPipeline")
                .dockerCredentials(List.of(
                        DockerCredential.dockerHub(
                                Secret.fromSecretCompleteArn(this, "DockerHubSecret", "arn:aws:secretsmanager:us-east-1:697816050693:secret:docker-7Ibngx")
                        )
                ))
                .synth(ShellStep.Builder
                        .create("Synth")
                        .input(source)
                        .commands(Arrays.asList(
                                "npm install -g aws-cdk",
                                "cd infrastructure",
                                "mvn clean install",
                                "cdk synth"
                        ))
                        .primaryOutputDirectory("infrastructure/cdk.out")
                        .build())
                .build();

        StageProps stageProps = StageProps.builder().env(props.getEnv()).build();

        pipeline.addStage(new InfraStage(this, "infra", stageProps));
        pipeline.addStage(new ApiGatewayServiceStage(this, "api-gateway-service", stageProps));
        pipeline.addStage(new AuthServiceStage(this, "auth-service", stageProps));
        pipeline.addStage(new UserServiceStage(this, "user-service", stageProps));
        pipeline.addStage(new SlotServiceStage(this, "slot-service", stageProps));
        pipeline.addStage(new OpenHourServiceStage(this, "open-hour-service", stageProps));
        pipeline.addStage(new EmailTokenServiceStage(this, "email-token-service", stageProps));
        pipeline.addStage(new NotificationServiceStage(this, "notification-service", stageProps));
    }
}
