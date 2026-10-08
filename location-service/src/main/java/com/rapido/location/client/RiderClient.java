package com.rapido.location.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Rider Service is the source of truth for rider AVAILABILITY.
 * Location Service only stores coordinates. Before returning nearby riders
 * to the Booking Service, we intersect the GeoRadius results with the set of
 * currently ONLINE riders (who also have a registered vehicle).
 */
@Component
public class RiderClient {

    private final RestClient restClient;

    public RiderClient(@Qualifier("riderRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Set<Long> getOnlineRiderIds() {
        try {
            List<Map<String, Object>> available = restClient.get()
                    .uri("/riders/internal/available")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (available == null) {
                return Set.of();
            }
            return available.stream()
                    .map(row -> (Number) row.get("riderId"))
                    .filter(Objects::nonNull)
                    .map(Number::longValue)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            // Without availability info we cannot guarantee correctness;
            // fail safe by returning an empty set (no riders dispatched).
            return Set.of();
        }
    }
}
