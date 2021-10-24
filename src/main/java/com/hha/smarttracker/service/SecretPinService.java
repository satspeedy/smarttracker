package com.hha.smarttracker.service;

import com.azure.core.util.polling.SyncPoller;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import io.dapr.client.DaprClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.hha.smarttracker.config.DaprConfiguration.DAPR_SECRET_STORE_NAME;

@Slf4j
@Service
public class SecretPinService {

    public final SecretClient secretClient;

    private final DaprClient daprClient;

    SecretPinService(SecretClient secretClient, DaprClient daprClient) {
        this.secretClient = secretClient;
        this.daprClient = daprClient;
    }

    /**
     * Create a secret tracking number pin valid for 1 week. If the secret
     * already exists in the key vault, then a new version of the secret is created.
     *
     * @param trackingNr trackingNr
     */
    public void setSecretTrackingPin(Long trackingNr, String trackingPin) {
        secretClient.setSecret(new KeyVaultSecret(
                String.valueOf(trackingNr), trackingPin)
                .setProperties(new SecretProperties()
                        .setExpiresOn(OffsetDateTime.now().plusWeeks(1))));
        log.info("Secret saved with key {}", trackingNr);
    }

    public Optional<String> getSecretTrackingPin(Long trackingNr) {
        Map<String, String> secretMap;
        try {
            secretMap = daprClient
                    .getSecret(DAPR_SECRET_STORE_NAME, String.valueOf(trackingNr))
                    .block();
        } catch (RuntimeException e) {
            log.error("Caught exception for key {}: {}", trackingNr, e.getMessage());
            log.info("Secret returned no entry for key {}", trackingNr);
            return Optional.empty();
        }
        Optional<String> optKey = secretMap.keySet().stream().findFirst();
        if (optKey.isEmpty()) {
            log.info("Secret returned no entry for key {}", trackingNr);
            return Optional.empty();
        }
        String key = optKey.get();
        String value = secretMap.get(key);
        log.info("Secret returned with key {} and value {}", key, value);
        return Optional.of(value);
    }

    public void deleteSecretTrackingPin(Long trackingNr) {
        SyncPoller<DeletedSecret, Void> deletionPoller = secretClient
                .beginDeleteSecret(String.valueOf(trackingNr));
        deletionPoller.waitForCompletion();
        log.info("Secret deleted with key {}", trackingNr);
    }
}
