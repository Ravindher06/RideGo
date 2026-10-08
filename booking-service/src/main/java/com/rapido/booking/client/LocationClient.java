package com.rapido.booking.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * Dispatch integration. Returns nearby ONLINE riders (nearest first) from
 * the Location Service's Redis GEO index. When the Location Service is down
 * or has no points yet, the caller falls back to the Rider Service's flat
 * online list, so dispatch never hard-depends on Redis.
 */
@Component
public class LocationClient {

    private final RestClient restClient;

    public LocationClient(@Qualifier("locationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<Long> findNearbyRiderIds(double latitude, double longitude, double radiusKm) {
        try {
            List<Long> nearby = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/locations/internal/nearby/{lat}/{lon}")
                            .queryParam("radiusKm", radiusKm)
                            .build(latitude, longitude))
                    .retrieve()
                    .body(List.class);
            return nearby != null ? nearby : Collections.emptyList();
        } catch (Exception ex) {
            // Non-fatal by design: the caller degrades gracefully to
            // Rider Service availability, which remains the authority.
            return Collections.emptyList();
        }
    }

    public void updateRiderLocation(Long riderId, Double latitude, Double longitude) {
        try {
            restClient.post()
                    .uri("/locations/rider")
                    .body(java.util.Map.of("riderId", riderId, "latitude", latitude, "longitude", longitude))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            // Location pings are ephemeral; the next heartbeat overwrites
            // anything missed. Log and continue.
        }
    }
}
