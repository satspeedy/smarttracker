package com.hha.smarttracker.messaging;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.Message;

@MessagingGateway
public interface MqttEventGateway {

    String MQTT_OUTBOUND_FLOW_INPUT = "mqttOutboundFlow.input";

    @Gateway(requestChannel = MQTT_OUTBOUND_FLOW_INPUT)
    void send(Message message);

}
