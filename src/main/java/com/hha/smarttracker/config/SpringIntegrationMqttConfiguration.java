package com.hha.smarttracker.config;

import com.hha.smarttracker.messaging.event.TrackingPositionChangedEvent;
import com.hha.smarttracker.service.StringifyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;

@Slf4j
@Configuration
public class SpringIntegrationMqttConfiguration {

    private static final String PREFIX_TCP_URL = "tcp://";

    @Value("${mqtt.broker.host}")
    private String mqttBrokerHost;

    @Value("${mqtt.broker.port}")
    private String mqttBrokerPort;

    @Value("${mqtt.client.id}")
    private String mqttClientId;

    @Value("${mqtt.test.client.id}")
    private String mqttTestClientId;

    @Value("${mqtt.broker.topicToListen}")
    private String topicToListen;

    @Value("${mqtt.broker.testTopicToListen}")
    private String testTopicToListen;

    @Bean
    public IntegrationFlow mqttInboundFlow() {
        MqttPahoMessageDrivenChannelAdapter adapter = getMqttAdapter(mqttClientId, topicToListen);
        return IntegrationFlows
                .from(adapter)
                .transform(Transformers.fromJson(TrackingPositionChangedEvent.class))
                .wireTap(f -> f.handle(m -> log.info("##### -> Inbound event received for client id '{}'" +
                                "from topic '{}': {}",
                        mqttClientId, topicToListen, StringifyHelper.toJson(m.getPayload()))))
                .handle("trackingService", "partialUpdateByEvent")
                .get();
    }

    @Bean
    public IntegrationFlow mqttInboundTestFlow() {
        MqttPahoMessageDrivenChannelAdapter adapter = getMqttAdapter(mqttTestClientId, testTopicToListen);
        return IntegrationFlows
                .from(adapter)
                .transform(Transformers.fromJson(TrackingPositionChangedEvent.class))
                .wireTap(f -> f.handle(m -> log.info("##### -> Inbound event received for client id '{}'" +
                                "from topic '{}': {}",
                        mqttTestClientId, testTopicToListen, StringifyHelper.toJson(m.getPayload()))))
                .get();
    }

    private MqttPahoMessageDrivenChannelAdapter getMqttAdapter(String clientId, String topic) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                PREFIX_TCP_URL + mqttBrokerHost + ":" + mqttBrokerPort,
                clientId, topic);
        adapter.setQos(1);
        return adapter;
    }

    @Bean
    public IntegrationFlow mqttOutboundFlow() {
        return f -> f
                .handle(new MqttPahoMessageHandler(
                        PREFIX_TCP_URL + mqttBrokerHost + ":" + mqttBrokerPort,
                        mqttClientId));
    }

}
