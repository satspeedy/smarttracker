package com.hha.smarttracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hha.smarttracker.model.enumeration.TrackingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@RedisHash("Trackings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tracking implements Serializable {

    @Schema(accessMode = READ_ONLY)
    @Id
    private Long trackingNumber;

    @Positive
    @Size(min = 4, max = 4)
    @Digits(integer=4, fraction=0)
    @Transient
    private String pin;

    @Schema(accessMode = READ_ONLY)
    private TrackingStatus trackingStatus;

    private TrackingStep currentPosition;

    @Schema(accessMode = READ_ONLY)
    private List<TrackingStep> history;


}
