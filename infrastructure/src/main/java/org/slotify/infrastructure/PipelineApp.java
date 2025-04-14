package org.slotify.infrastructure;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class PipelineApp {
    public static void main(String[] args) {
        App app = new App();

        StackProps props = StackProps.builder()
                .env(Environment.builder()
                        .account("697816050693")
                        .region("us-east-1")
                        .build())
                .build();

        new PipelineStack(app, "SlotifyPipelineStack", props);
        app.synth();
    }
}
