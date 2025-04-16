package org.slotify.infrastructure;

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
                        .connectionArn("arn:aws:codeconnections:us-east-1:697816050693:connection/5173abe2-3be8-4fe9-809c-0ae4e3656028")
                        .build()
        );

        CodePipeline pipeline = CodePipeline.Builder.create(this, "SlotifyPipeline")
                .pipelineName("SlotifyPipeline")
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
                .dockerCredentials(List.of(
                        DockerCredential.dockerHub(
                                Secret.fromSecretNameV2(this, "DockerHubSecret", "docker")
                        )
                ))
                .build();

        StageProps stageProps = StageProps.builder().env(props.getEnv()).build();

        pipeline.addStage(new AppStage(this, "DeploySlotifyApp", stageProps));
    }
}
