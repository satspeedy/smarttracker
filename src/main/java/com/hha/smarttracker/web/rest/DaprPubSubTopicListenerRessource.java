package com.hha.smarttracker.web.rest;

import com.hha.smarttracker.service.StringifyHelper;
import com.hha.smarttracker.service.TrackingService;
import com.hha.smarttracker.web.rest.dto.DeliveryDeletedEvent;
import io.dapr.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static com.hha.smarttracker.config.DaprConfiguration.DAPR_PUBSUB_NAME;
import static com.hha.smarttracker.config.DaprConfiguration.DAPR_PUBSUB_TOPIC_NAME_DELIVERY_DELETED;

@Slf4j
@RestController
public class DaprPubSubTopicListenerRessource {

    private final TrackingService trackingService;

    public DaprPubSubTopicListenerRessource(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Topic(name = DAPR_PUBSUB_TOPIC_NAME_DELIVERY_DELETED, pubsubName = DAPR_PUBSUB_NAME)
    @PostMapping(path = "/" + DAPR_PUBSUB_TOPIC_NAME_DELIVERY_DELETED)
    public ResponseEntity<String> handleDeliveryDeletedEvent(@RequestBody(required = false) DeliveryDeletedEvent event) {
        if (event != null && event.getEventId() != null && event.getTrackingNumber() != null) {
            logConsumedMessage(DAPR_PUBSUB_NAME, DAPR_PUBSUB_TOPIC_NAME_DELIVERY_DELETED,
                    event.getEventId(), StringifyHelper.toJson(event));
            trackingService.delete(event.getTrackingNumber());
        } else {
            log.error("Event is null or doesnt contain enough data to proceed: {}", event);
        }
        return Optional.ofNullable(event).map(response -> ResponseEntity.ok()
                        .body(StringifyHelper.toJson(event)))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));
    }

    private void logConsumedMessage(String pubsubName, String topicName, String key, String message) {
        log.info("##### -> Consumed via Dapr component {} topic {} key {} with message -> {}",
                pubsubName, topicName, key, message);
    }

}


