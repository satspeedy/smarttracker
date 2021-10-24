package com.hha.smarttracker.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonDeserialize(builder = DeliveryDeletedEvent.DeliveryDeletedEventBuilder.class)
@Builder(builderClassName = "DeliveryDeletedEventBuilder", toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryDeletedEvent {

    private String eventId;

    private LocalDateTime eventDateTime;

    private Long deliveryId;

    private Long trackingNumber;

    @JsonPOJOBuilder(withPrefix = "")
    public static class DeliveryDeletedEventBuilder {
    }

}

