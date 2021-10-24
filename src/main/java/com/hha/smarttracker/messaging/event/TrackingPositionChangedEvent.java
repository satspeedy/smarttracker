package com.hha.smarttracker.messaging.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.hha.smarttracker.messaging.event.enumeration.TrackingSignal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonDeserialize(builder = TrackingPositionChangedEvent.TrackingPositionChangedEventBuilder.class)
@Builder(builderClassName = "TrackingPositionChangedEventBuilder", toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingPositionChangedEvent {

    private String eventId;

    private LocalDateTime eventDateTime;

    private Long trackingNumber;

    private TrackingSignal trackingSignal;

    private double latitude;

    private double longitude;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TrackingPositionChangedEventBuilder {
    }
}
