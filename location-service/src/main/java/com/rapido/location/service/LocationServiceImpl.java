package com.rapido.location.service;

import com.rapido.location.client.RiderClient;
import com.rapido.location.dto.LocationResponse;
import com.rapido.location.dto.NearbyRiderResponse;
import com.rapido.location.dto.RiderLocationRequest;
import com.rapido.location.exception.LocationServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Why Redis for locations?
 * - Rider coordinates change every few seconds; writing each ping to PostgreSQL
 *   would generate unbounded churn and index pressure on a relational table.
 * - Redis GEO (GEORADIUS/GEODIST) resolves "riders within X km" in a single
 *   in-memory O(log n) command with a bounding box.
 * - Coordinates are ephemeral by nature; losing a stale point is acceptable
 *   because the next heartbeat overwrites it. Rider status remains
 *   authoritative in the Rider Service, so Redis never double-owns state.
 */
@Service
public class LocationServiceImpl implements LocationService {

    private static final String GEO_KEY = "rapido:rider:location";
    private static final String META_KEY = "rapido:rider:location:meta";
    private static final int MAX_RADIUS_RESULTS = 50;

    private final RedisTemplate<String, String> redisTemplate;
    private final RiderClient riderClient;
    private final long staleSeconds;

    public LocationServiceImpl(RedisTemplate<String, String> redisTemplate,
                               RiderClient riderClient,
                               @Value("${rapido.location-stale-seconds}") long staleSeconds) {
        this.redisTemplate = redisTemplate;
        this.riderClient = riderClient;
        this.staleSeconds = staleSeconds;
    }

    @Override
    public boolean updateRiderLocation(RiderLocationRequest request) {
        try {
            GeoOperations<String, String> geo = redisTemplate.geoOperations();
            // Redis Point uses (x = longitude, y = latitude)
            Point point = new Point(request.getLongitude(), request.getLatitude());
            geo.add(GEO_KEY, point, String.valueOf(request.getRiderId()));

            HashOperations<String, String, String> meta = redisTemplate.opsForHash();
            meta.put(META_KEY, String.valueOf(request.getRiderId()), Instant.now().toString());
            return true;
        } catch (Exception ex) {
            throw new LocationServiceUnavailableException("Failed to update rider location: " + ex.getMessage());
        }
    }

    @Override
    public LocationResponse getRiderLocation(Long riderId) {
        try {
            GeoOperations<String, String> geo = redisTemplate.geoOperations();
            Point point = geo.position(GEO_KEY, String.valueOf(riderId));
            if (point == null) {
                throw new LocationServiceUnavailableException("No known location for rider " + riderId);
            }

            LocationResponse response = new LocationResponse();
            response.setRiderId(riderId);
            response.setLatitude(point.getY());
            response.setLongitude(point.getX());

            HashOperations<String, String, String> meta = redisTemplate.opsForHash();
            Object lastUpdated = meta.get(META_KEY, String.valueOf(riderId));
            if (lastUpdated != null) {
                response.setLastUpdated(String.valueOf(lastUpdated));
                Instant updated = Instant.parse(String.valueOf(lastUpdated));
                response.setStale(Duration.between(updated, Instant.now()).getSeconds() > staleSeconds);
            } else {
                response.setStale(true);
            }
            return response;
        } catch (LocationServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LocationServiceUnavailableException("Failed to read rider location: " + ex.getMessage());
        }
    }

    @Override
    public List<NearbyRiderResponse> findNearbyRiders(double latitude, double longitude, double radiusKm) {
        List<NearbyRiderResponse> candidates;
        try {
            candidates = radiusSearch(latitude, longitude, radiusKm);
        } catch (LocationServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LocationServiceUnavailableException("GeoRadius search failed: " + ex.getMessage());
        }

        Set<Long> onlineRiderIds = riderClient.getOnlineRiderIds();
        return candidates.stream()
                .filter(rider -> onlineRiderIds.contains(rider.getRiderId()))
                .sorted(Comparator.comparingDouble(NearbyRiderResponse::getDistanceKm))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findNearbyRiderIds(double latitude, double longitude, double radiusKm) {
        return findNearbyRiders(latitude, longitude, radiusKm).stream()
                .map(NearbyRiderResponse::getRiderId)
                .collect(Collectors.toList());
    }

    private List<NearbyRiderResponse> radiusSearch(double latitude, double longitude, double radiusKm) {
        GeoOperations<String, String> geo = redisTemplate.geoOperations();
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radiusKm, Metrics.KILOMETERS);

        GeoResults<String> results = geo.radius(GEO_KEY, center, distance, Limit.of(MAX_RADIUS_RESULTS));
        List<NearbyRiderResponse> nearby = new ArrayList<>();
        for (GeoResult<String> result : results.getContent()) {
            try {
                Long riderId = Long.valueOf(result.getContent());
                NearbyRiderResponse response = new NearbyRiderResponse();
                response.setRiderId(riderId);
                response.setDistanceKm(Double.parseDouble(result.getDistance().toKilometers().toPlainString()));
                nearby.add(response);
            } catch (NumberFormatException ignored) {
                // Skip malformed members defensively
            }
        }

        // Hydrate actual coordinates in a single round trip
        if (!nearby.isEmpty()) {
            String[] members = nearby.stream()
                    .map(rider -> String.valueOf(rider.getRiderId()))
                    .toArray(String[]::new);
            List<Point> points = geo.position(GEO_KEY, members);
            for (int i = 0; i < nearby.size() && i < points.size(); i++) {
                Point point = points.get(i);
                if (Objects.nonNull(point)) {
                    nearby.get(i).setLatitude(point.getY());
                    nearby.get(i).setLongitude(point.getX());
                }
            }
        }
        return nearby;
    }
}
