package org.slotify.infrastructure;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.StageProps;
import software.amazon.awscdk.pipelines.*;
import software.constructs.Construct;

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
                .synth(new ShellStep("Synth",
                        ShellStepProps.builder()
                                .input(source)
                                .commands(List.of(
                                        "cd infrastructure",
                                        "mvn clean install",
                                        "npm install aws-cdk-lib",
                                        "npx cdk --version",  // Debug command
                                        "npx cdk synth"
                                ))
                                .primaryOutputDirectory("infrastructure/cdk.out")  // Specify the output directory
                                .build()))
                .build();

        StageProps stageProps = StageProps.builder().env(props.getEnv()).build();

        pipeline.addStage(new AppStage(this, "DeploySlotifyApp", stageProps));
    }
}
