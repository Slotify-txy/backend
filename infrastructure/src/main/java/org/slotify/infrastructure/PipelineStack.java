package org.slotify.infrastructure;

import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ConnectionSourceOptions;
import software.amazon.awscdk.pipelines.ShellStep;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Map;

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

        SecretValue dockerUsername = SecretValue.secretsManager("docker-username");
        SecretValue dockerPassword = SecretValue.secretsManager("docker-password");

        CodePipeline pipeline = CodePipeline.Builder.create(this, "SlotifyPipeline")
                .pipelineName("SlotifyPipeline")
                .synth(ShellStep.Builder
                        .create("Synth")
                        .input(source)
                        .env(Map.of(
                                "DOCKER_HUB_USERNAME", dockerUsername.toString(),
                                "DOCKER_HUB_PASSWORD", dockerPassword.toString())
                        )
                        .commands(Arrays.asList(
                                "npm install -g aws-cdk",
                                "docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD",
                                "cd infrastructure",
                                "mvn clean install",
                                "cdk synth"
                        ))
                        .primaryOutputDirectory("infrastructure/cdk.out")
                        .build())
                .build();

        StageProps stageProps = StageProps.builder().env(props.getEnv()).build();

        pipeline.addStage(new AppStage(this, "DeploySlotifyApp", stageProps));
    }
}
