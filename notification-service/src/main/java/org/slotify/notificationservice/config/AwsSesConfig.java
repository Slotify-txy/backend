package org.slotify.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesAsyncClient;

@Configuration
public class AwsSesConfig {
    @Value("${spring.cloud.aws.region.static}")
    private String region;

    @Bean
    public SesAsyncClient sesAsyncClient() {
        return SesAsyncClient.builder()
                .region(Region.of(region))
                .build();
    }
}
