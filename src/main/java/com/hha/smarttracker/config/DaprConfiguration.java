package com.hha.smarttracker.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DaprConfiguration {

    private static final DaprClientBuilder BUILDER = new DaprClientBuilder();

    public static final String DAPR_PUBSUB_NAME = "pubsub-kafka-rentadrone";
    public static final String DAPR_PUBSUB_TOPIC_NAME_DELIVERY_DELETED = "delivery-deleted";

    public static final String DAPR_STATE_STORE_NAME = "statestore";
    public static final String DAPR_STATE_STORE_KEY_PREFIX = "Tracking:";

    public static final String DAPR_SECRET_STORE_NAME = "secret-store-azurekeyvault-rentadrone";

    @Bean
    public DaprClient buildDaprClient() {
        return BUILDER.build();
    }

}
