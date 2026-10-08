package com.rapido.location.service;

import com.rapido.location.dto.LocationResponse;
import com.rapido.location.dto.NearbyRiderResponse;
import com.rapido.location.dto.RiderLocationRequest;

import java.util.List;

public interface LocationService {
    boolean updateRiderLocation(RiderLocationRequest request);

    LocationResponse getRiderLocation(Long riderId);

    /**
     * Radius search around a pickup point. Results are filtered to riders
     * that are currently ONLINE according to the Rider Service, and ordered
     * by distance (nearest first).
     */
    List<NearbyRiderResponse> findNearbyRiders(double latitude, double longitude, double radiusKm);

    /**
     * Lightweight variant used by the Booking Service for dispatch.
     * Returns only rider IDs, nearest first.
     */
    List<Long> findNearbyRiderIds(double latitude, double longitude, double radiusKm);
}
