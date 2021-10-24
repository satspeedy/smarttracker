package com.hha.smarttracker.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpringIntegrationCommonMqttEventListeners {

    @EventListener
    void handleMqttConnectionFailedEvent(MqttConnectionFailedEvent event) {
        log.info("##### - Mqtt Connection Failed Event: {}", event);
    }

    @EventListener
    void handleMqttMessageSentEvent(MqttMessageSentEvent event) {
        log.info("##### <- Mqtt Message Sent Event: {}", event);
    }

    @EventListener
    void handleMqttMessageDeliveredEvent(MqttMessageDeliveredEvent event) {
        log.info("##### -> Mqtt Message Delivered Event: {}", event);
    }

    @EventListener
    void handleMqttSubscribedEvent(MqttSubscribedEvent event) {
        log.info("##### - Mqtt Subscribed Event: {}", event);
    }

}
