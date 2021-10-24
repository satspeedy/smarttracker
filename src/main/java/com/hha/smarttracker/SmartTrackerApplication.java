package com.hha.smarttracker;

import com.github.javafaker.Faker;
import com.hha.smarttracker.messaging.MqttEventGateway;
import com.hha.smarttracker.messaging.event.TrackingPositionChangedEvent;
import com.hha.smarttracker.messaging.event.enumeration.TrackingSignal;
import com.hha.smarttracker.service.StringifyHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.text.NumberFormat;
import java.time.LocalDateTime;

@Slf4j
@SpringBootApplication
@EnableCaching
@IntegrationComponentScan
public class SmartTrackerApplication {

    @Value("${mqtt.broker.testTopicToListen}")
    private String testTopicToListen;

    private final Faker faker = new Faker();

    public static final String ENV_VAR_AZURE_CLIENT_ID = "AZURE_CLIENT_ID";
    public static final String ENV_VAR_AZURE_CLIENT_SECRET = "AZURE_CLIENT_SECRET";
    public static final String ENV_VAR_AZURE_TENANT_ID = "AZURE_TENANT_ID";
    public static final String ENV_VAR_AZURE_VAULT_URL = "AZURE_VAULT_URL";

    @Autowired
    private ConfigurableApplicationContext ctx;

    public static void main(String[] args) {
        checkRequiredEnvVariables();
        SpringApplication.run(SmartTrackerApplication.class, args);
    }

    private static void checkRequiredEnvVariables() {
        if (isEnvVarSet(ENV_VAR_AZURE_CLIENT_ID) && isEnvVarSet(ENV_VAR_AZURE_CLIENT_SECRET) &&
                isEnvVarSet(ENV_VAR_AZURE_TENANT_ID) && isEnvVarSet(ENV_VAR_AZURE_VAULT_URL)) {
            log.info("All required environment variables are given.");
        } else {
            throw new IllegalStateException("Environment variables are required to set before running this app: "
                    + ENV_VAR_AZURE_CLIENT_ID
                    + ", "
                    + ENV_VAR_AZURE_CLIENT_SECRET
                    + ", "
                    + ENV_VAR_AZURE_TENANT_ID
                    + ", "
                    + ENV_VAR_AZURE_VAULT_URL
                    + ".");
        }
    }

    private static boolean isEnvVarSet(String envVar) {
        return System.getenv(envVar) != null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAfterStartup() {
        for (int i = 0; i < 5; i++) {
            TrackingPositionChangedEvent event = buildExampleEvent();
            sendExampleEvent(event, testTopicToListen);
        }
    }

    @SneakyThrows
    private TrackingPositionChangedEvent buildExampleEvent() {
        NumberFormat nf = NumberFormat.getInstance();

        return TrackingPositionChangedEvent.builder()
                .eventId(faker.number().digits(20))
                .eventDateTime(LocalDateTime.now())
                .trackingNumber(faker.number().randomNumber())
                .trackingSignal(TrackingSignal.START)
                .latitude(nf.parse(faker.address().latitude()).doubleValue())
                .longitude(nf.parse(faker.address().longitude()).doubleValue())
                .build();
    }

    private void sendExampleEvent(TrackingPositionChangedEvent event, String topicName) {
        MqttEventGateway mqttEventGateway = ctx.getBean(MqttEventGateway.class);

        Message<String> message = MessageBuilder
                .withPayload(StringifyHelper.toJson(event))
                .setHeader(MqttHeaders.TOPIC, topicName)
                .setHeader("chuckNorrisFact", faker.chuckNorris().fact())
                .build();
        mqttEventGateway.send(message);
    }
}
