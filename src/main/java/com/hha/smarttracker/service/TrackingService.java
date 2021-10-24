package com.hha.smarttracker.service;

import com.hha.smarttracker.config.SpringIntegrationMqttConfiguration;
import com.hha.smarttracker.messaging.event.TrackingPositionChangedEvent;
import com.hha.smarttracker.messaging.event.enumeration.TrackingSignal;
import com.hha.smarttracker.model.Tracking;
import com.hha.smarttracker.model.TrackingStep;
import com.hha.smarttracker.model.enumeration.LocationName;
import com.hha.smarttracker.model.enumeration.TrackingStatus;
import com.hha.smarttracker.repository.TrackingRepository;
import io.dapr.client.DaprClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.hha.smarttracker.config.DaprConfiguration.DAPR_STATE_STORE_KEY_PREFIX;
import static com.hha.smarttracker.config.DaprConfiguration.DAPR_STATE_STORE_NAME;
import static com.hha.smarttracker.messaging.event.enumeration.TrackingSignal.END;
import static com.hha.smarttracker.messaging.event.enumeration.TrackingSignal.START;

@Slf4j
@Service
@Transactional
public class TrackingService {

    /**
     * Syntax: https://www.google.com/maps/search/?api=1&query=<lat>,<lng>
     * See https://developers.google.com/maps/documentation/urls/guide for more infos.
     **/
    private static final String GOOGLE_MAPS_SEARCH_URL = "https://www.google.com/maps/search/?api=1&query=";

    private final TrackingRepository trackingRepository;

    private final SecretPinService secretPinService;

    private final DaprClient daprClient;

    public TrackingService(TrackingRepository trackingRepository, SecretPinService secretPinService, DaprClient daprClient) {
        this.trackingRepository = trackingRepository;
        this.secretPinService = secretPinService;
        this.daprClient = daprClient;
    }

    /**
     * Save a tracking.
     *
     * @param tracking the entity to save.
     * @return the persisted entity.
     */
    @SneakyThrows
    public Tracking save(Tracking tracking) {
        log.info("Request to save tracking: {}", StringifyHelper.toJson(tracking));
        tracking.setHistory(new ArrayList<>());
        if (tracking.getCurrentPosition() != null) {
            enrichTrackingStepWithGoogleMapsUrl(tracking.getCurrentPosition());
            addCurrentPositionToHistory(tracking.getCurrentPosition(), tracking);
        }
        updateTrackingStatus(tracking, null);
        Tracking saved = trackingRepository.save(tracking);
        saveTrackingInSharedStateStore(saved);
        if (Optional.ofNullable(tracking.getPin()).isPresent()) {
            saved.setPin(tracking.getPin());
            saveSecretTrackingPin(saved.getTrackingNumber(), saved.getPin());
        }
        return saved;
    }

    private void updateTrackingStatus(Tracking tracking, TrackingStatus existingTrackingStatus) {
        if (tracking.getCurrentPosition() != null) {
            tracking.setTrackingStatus(TrackingStatus.LIVE);
            LocationName name = tracking.getCurrentPosition().getLocationName();
            if (name != null && name.equals(LocationName.END_LOCATION)) {
                tracking.setTrackingStatus(TrackingStatus.COMPLETED);
            }
        } else {
            tracking.setTrackingStatus(Objects.requireNonNullElse(existingTrackingStatus, TrackingStatus.IN_PREPARATION));
        }
    }

    private void addCurrentPositionToHistory(TrackingStep currentPosition, Tracking tracking) {
        if (tracking.getHistory() == null) {
            tracking.setHistory(new ArrayList<>());
        }
        List<TrackingStep> history = tracking.getHistory();
        history.add(0, currentPosition);
    }

    private void enrichTrackingStepWithGoogleMapsUrl(TrackingStep step) {
        if (step != null && step.getLatitude() != null && step.getLongitude() != null) {
            step.setGoogleMapsUrl(generateGoogleMapsSearchUrl(step.getLatitude(), step.getLongitude()).toString());
        }
    }

    @SneakyThrows
    private URL generateGoogleMapsSearchUrl(double latitude, double longitude) {
        return new URL(GOOGLE_MAPS_SEARCH_URL + latitude + "," + longitude);
    }

    /**
     * Called from {@link SpringIntegrationMqttConfiguration#mqttInboundFlow()}
     * @param e event
     */
    @SneakyThrows
    public void partialUpdateByEvent(TrackingPositionChangedEvent e) {
        log.info("Event received to partially update tracking: {}", StringifyHelper.toJson(e));
        Tracking tracking = Tracking.builder()
                .trackingNumber(e.getTrackingNumber())
                .currentPosition(TrackingStep.builder()
                        .latitude(e.getLatitude())
                        .longitude(e.getLongitude())
                        .locationName(setLocationName(e.getTrackingSignal()))
                        .build()
                )
                .build();
        partialUpdate(tracking);
    }

    private LocationName setLocationName(TrackingSignal signal) {
        if (signal == null) {
            return null;
        }
        if (signal.equals(START)) {
            return LocationName.START_LOCATION;
        } else if (signal.equals(END)) {
            return LocationName.END_LOCATION;
        }
        return null;
    }

    /**
     * Partially update a tracking.
     *
     * @param tracking the entity to update partially.
     * @return the persisted entity.
     */
    @SneakyThrows
    public Optional<Tracking> partialUpdate(Tracking tracking) {
        log.info("Request to partially update tracking: {}", StringifyHelper.toJson(tracking));

        return trackingRepository
                .findById(tracking.getTrackingNumber())
                .map(existingTracking -> {
                            updateTrackingStatus(tracking, existingTracking.getTrackingStatus());
                            if (tracking.getTrackingStatus() != null) {
                                existingTracking.setTrackingStatus(tracking.getTrackingStatus());
                            }
                            if (tracking.getPin() != null) {
                                saveSecretTrackingPin(existingTracking.getTrackingNumber(), tracking.getPin());
                            }
                            if (tracking.getCurrentPosition() != null) {
                                enrichTrackingStepWithGoogleMapsUrl(tracking.getCurrentPosition());
                                existingTracking.setCurrentPosition(tracking.getCurrentPosition());
                                addCurrentPositionToHistory(tracking.getCurrentPosition(), existingTracking);
                            }
                            return existingTracking;
                        }
                )
                .map(trackingRepository::save)
                .map(t -> {
                    saveTrackingInSharedStateStore(t);
                    return t;
                })
                .map(this::enrichWithSecretTrackingPin);
    }

    private void saveSecretTrackingPin(Long trackingNr, String trackingPin) {
        secretPinService.setSecretTrackingPin(trackingNr, trackingPin);
    }

    private Tracking enrichWithSecretTrackingPin(Tracking tracking) {
        Optional<String> optPin = secretPinService.getSecretTrackingPin(tracking.getTrackingNumber());
        optPin.ifPresent(tracking::setPin);
        return tracking;
    }

    /**
     * Get all the trackings.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<Tracking> findAll() {
        log.info("Request to get all trackings");
        List<Tracking> all = (List<Tracking>) trackingRepository.findAll();
        all.forEach(this::enrichWithSecretTrackingPin);
        return all;
    }

    /**
     * Get one tracking by id.
     *
     * @param trackingNumber the trackingNumber of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Tracking> find(Long trackingNumber) {
        log.info("Request to get tracking: {}", trackingNumber);
        Optional<Tracking> opt = trackingRepository.findById(trackingNumber);
        if (opt.isEmpty()) {
            return opt;
        } else {
            return Optional.of(enrichWithSecretTrackingPin(opt.get()));
        }
    }

    /**
     * Delete the tracking by trackingNumber.
     *
     * @param trackingNumber the trackingNumber of the entity.
     */
    public void delete(Long trackingNumber) {
        log.info("Request to delete tracking: {}", trackingNumber);
        trackingRepository.deleteById(trackingNumber);
        secretPinService.deleteSecretTrackingPin(trackingNumber);
        deleteTrackingFromSharedStateStore(trackingNumber);
    }

    private void saveTrackingInSharedStateStore(Tracking tracking) {
        daprClient.saveState(DAPR_STATE_STORE_NAME, DAPR_STATE_STORE_KEY_PREFIX + tracking.getTrackingNumber(), tracking).block();
        log.info("Saved current tracking state in shared store: {}", StringifyHelper.toJson(tracking));
    }

    private void deleteTrackingFromSharedStateStore(Long trackingNumber) {
        daprClient.deleteState(DAPR_STATE_STORE_NAME, DAPR_STATE_STORE_KEY_PREFIX + trackingNumber).block();
        log.info("Deleted current tracking state from shared store: {}", trackingNumber);
    }

}
