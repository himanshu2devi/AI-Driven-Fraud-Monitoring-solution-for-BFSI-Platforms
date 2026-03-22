package com.wipro.fraud.aiassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;

@Configuration
public class BedrockClientConfig {

    @Bean
    public BedrockAgentRuntimeClient bedrockClient() {

        return BedrockAgentRuntimeClient.builder()
                .region(Region.EU_NORTH_1)
                .build();
    }
}