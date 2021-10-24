package com.hha.smarttracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.hha.smarttracker.model.enumeration.LocationName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

import java.io.Serializable;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackingStep implements Serializable {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private LocationName locationName;

    @Schema(accessMode = READ_ONLY)
    private String googleMapsUrl;

}
