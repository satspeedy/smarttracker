package com.hha.smarttracker.web.rest;

import com.hha.smarttracker.model.Tracking;
import com.hha.smarttracker.service.SecretPinService;
import com.hha.smarttracker.service.StringifyHelper;
import com.hha.smarttracker.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing {@link Tracking}.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class TrackingRessource {

    private final TrackingService trackingService;

    private final SecretPinService secretPinService;

    public TrackingRessource(TrackingService trackingService, SecretPinService secretPinService) {
        this.trackingService = trackingService;
        this.secretPinService = secretPinService;
    }

    /**
     * {@code POST  /trackings} : Create a new tracking.
     *
     * @param tracking the tracking to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new tracking, or with status {@code 400 (Bad Request)} if the tracking has already a trackingNumber.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @Operation(summary = "Create a new tracking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "400", description = "Has already a trackingNumber",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Couldn't be created",
                    content = @Content)})
    @PostMapping("/trackings")
    public ResponseEntity<Tracking> createTracking(@Valid @RequestBody Tracking tracking) throws URISyntaxException {
        log.info("REST request to save tracking: {}", StringifyHelper.toJson(tracking));
        if (tracking.getTrackingNumber() != null) {
            throw new IllegalStateException("A new tracking cannot already have a trackingNumber: tracking-number-exists");
        }
        Tracking result = trackingService.save(tracking);
        return ResponseEntity
                .created(new URI("/api/trackings/" + result.getTrackingNumber()))
                .body(result);
    }

    /**
     * {@code PUT  /trackings/:trackingNumber} : Updates an existing tracking.
     *
     * @param trackingNumber the trackingNumber of the tracking to save.
     * @param pin            the pin of the tracking to save.
     * @param tracking       the tracking to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tracking,
     * or with status {@code 400 (Bad Request)} if the tracking is not valid,
     * or with status {@code 401 (Unauthorized)} if the tracking pin is not valid,
     * or with status {@code 404 (Unauthorized)} if the tracking is not found,
     * or with status {@code 500 (Internal Server Error)} if the tracking couldn't be updated.
     */
    @Operation(summary = "Replace the complete existing tracking")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found and updated",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "400", description = "Not valid",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid Pin",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Couldn't be updated",
                    content = @Content)})
    @PutMapping("/trackings/{trackingNumber}")
    public ResponseEntity<Tracking> updateTracking(
            @Parameter(description = "the trackingNumber of the tracking to update") @PathVariable(value = "trackingNumber", required = false) final Long trackingNumber,
            @Parameter(description = "the pin of the tracking to update") @RequestParam(value = "pin") final String pin,
            @Valid @RequestBody Tracking tracking) {
        log.info("REST request to update tracking : {}, {}", trackingNumber, StringifyHelper.toJson(tracking));
        checkTrackingNumber(trackingNumber, tracking);
        checkPin(trackingNumber, pin);

        Tracking result = trackingService.save(tracking);
        return ResponseEntity
                .ok()
                .body(result);
    }

    private void checkPin(Long trackingNumber, String enteredPin) {
        if (enteredPin == null) {
            throw new IllegalStateException(
                    "Invalid entered tracking pin: entered-tracking-pin-null");
        }
        secretPinService.getSecretTrackingPin(trackingNumber);
        Optional<String> optPin = secretPinService.getSecretTrackingPin(trackingNumber);
        if (optPin.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid entered tracking trackingNumber: entered-tracking-number-not-found");
        }
        String pin = optPin.get();
        if (!Objects.equals(pin, enteredPin)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "" +
                    "Invalid entered tracking pin: entered-tracking-pin-wrong");
        }
    }

    private void checkTrackingNumber(Long trackingNumber, @RequestBody @Valid Tracking tracking) {
        if (tracking.getTrackingNumber() == null) {
            throw new IllegalStateException("Invalid tracking trackingNumber: tracking-number-null");
        }
        if (!Objects.equals(trackingNumber, tracking.getTrackingNumber())) {
            throw new IllegalStateException("Invalid tracking trackingNumber: tracking-number-invalid");
        }
        if (trackingService.find(trackingNumber).isEmpty()) {
            throw new IllegalStateException("Invalid tracking trackingNumber: tracking-number-not-found");
        }
    }

    /**
     * {@code PATCH  /trackings/:trackingNumber} : Partial updates given fields of an existing tracking, field will ignore if it is null
     *
     * @param trackingNumber the trackingNumber of the tracking to save.
     * @param pin            the pin of the tracking to save.
     * @param tracking       the tracking to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tracking,
     * or with status {@code 400 (Bad Request)} if the tracking is not valid,
     * or with status {@code 401 (Unauthorized)} if the tracking pin is not valid,
     * or with status {@code 404 (Not Found)} if the tracking is not found,
     * or with status {@code 500 (Internal Server Error)} if the tracking couldn't be updated.
     */
    @Operation(summary = "Update only given fields of an existing tracking, field will ignore if it is null")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found and updated",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "400", description = "Not valid",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid Pin",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Couldn't be updated",
                    content = @Content)})
    @PatchMapping(value = "/trackings/{trackingNumber}")
    public ResponseEntity<Tracking> partialUpdateTracking(
            @Parameter(description = "the trackingNumber of the tracking to update") @PathVariable(value = "trackingNumber", required = false) final Long trackingNumber,
            @Parameter(description = "the pin of the tracking to update") @RequestParam(value = "pin") final String pin,
            @NotNull @RequestBody Tracking tracking) {
        log.info("REST request to partial update tracking partially : {}, {}", trackingNumber, StringifyHelper.toJson(tracking));
        checkTrackingNumber(trackingNumber, tracking);
        checkPin(trackingNumber, pin);

        Optional<Tracking> result = trackingService.partialUpdate(tracking);

        return result.map(response -> ResponseEntity.ok().body(response))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * {@code GET  /trackings} : get all the trackings.
     *
     * @param role users role to check access right.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of trackings in body
     * or with status {@code 403 (Forbidden)} if the user has no access rights.
     */
    @Operation(summary = "Get all trackings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the list of found objects in body",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "403", description = "Unauthorized user role",
                    content = @Content)})
    @GetMapping("/trackings")
    public ResponseEntity<List<Tracking>> getAllTrackings(@Parameter(description = "the role of the user") @RequestParam(value = "role") final String role) {
        log.info("REST request to get all trackings");
        if (role == null || !role.equals("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing role access rights: unauthorized-role");
        }
        return ResponseEntity.ok(trackingService.findAll());
    }

    /**
     * {@code GET  /trackings/:trackingNumber} : get the "trackingNumber" tracking.
     *
     * @param trackingNumber the trackingNumber of the tracking to retrieve.
     * @param pin            the pin of the tracking to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with the object in body, or with status {@code 404 (Not Found)},
     * or with status {@code 401 (Unauthorized)} if the tracking pin is not valid.
     */
    @Operation(summary = "Get a tracking by trackingNumber")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the tracking",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid trackingNumber supplied",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid Pin",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content)})
    @GetMapping("/trackings/{trackingNumber}")
    public ResponseEntity<Tracking> getTracking(
            @Parameter(description = "trackingNumber of the tracking to retrieve") @PathVariable Long trackingNumber,
            @Parameter(description = "the pin of the tracking to retrieve") @RequestParam(value = "pin") final String pin) {
        log.info("REST request to get tracking : {}", trackingNumber);
        checkPin(trackingNumber, pin);
        Optional<Tracking> tracking = trackingService.find(trackingNumber);
        return tracking.map(response -> ResponseEntity.ok().body(response))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * {@code DELETE  /trackings/:trackingNumber} : delete the "trackingNumber" tracking.
     *
     * @param trackingNumber the trackingNumber of the tracking to delete.
     * @param pin            the pin of the tracking to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)},
     * or with status {@code 401 (Unauthorized)} if the tracking pin is not valid.
     */
    @Operation(summary = "Delete a tracking by trackingNumber")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Found and deleted successful",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Tracking.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid trackingNumber supplied",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid Pin",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content)})
    @DeleteMapping("/trackings/{trackingNumber}")
    public ResponseEntity<Void> deleteTracking(
            @Parameter(description = "trackingNumber of the tracking to delete") @PathVariable Long trackingNumber,
            @Parameter(description = "the pin of the tracking to delete") @RequestParam(value = "pin") final String pin) {
        log.info("REST request to delete tracking: {}", trackingNumber);
        checkPin(trackingNumber, pin);
        trackingService.delete(trackingNumber);
        return ResponseEntity
                .noContent()
                .build();
    }

}
