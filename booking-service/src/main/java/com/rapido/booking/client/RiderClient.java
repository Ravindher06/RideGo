package com.rapido.booking.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RiderClient {

    private final RestClient restClient;

    public RiderClient(@Qualifier("riderRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean checkRiderExists(Long riderId) {
        try {
            Boolean exists = restClient.get()
                    .uri("/riders/internal/{id}/exists", riderId)
                    .retrieve()
                    .body(Boolean.class);
            return exists != null && exists;
        } catch (Exception ex) {
            throw new RuntimeException("Rider service is currently unreachable: " + ex.getMessage());
        }
    }

    public List<Map<String, Object>> getAvailableRiders() {
        try {
            return restClient.get()
                    .uri("/riders/internal/available")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            // Log & return empty list rather than hard crash during selection
            return Collections.emptyList();
        }
    }

    public void reserveRider(Long riderId) {
        updateRiderStatusIfCurrent(riderId, "ONLINE", "BUSY");
    }

    public void releaseRider(Long riderId) {
        updateRiderStatusIfCurrent(riderId, "BUSY", "ONLINE");
    }

    private void updateRiderStatusIfCurrent(Long riderId, String expectedStatus, String newStatus) {
        try {
            restClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/riders/internal/{id}/status")
                            .queryParam("status", newStatus)
                            .queryParam("expectedStatus", expectedStatus)
                            .build(riderId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new RuntimeException("Rider reservation/status update failed: " + ex.getMessage());
        }
    }
}
