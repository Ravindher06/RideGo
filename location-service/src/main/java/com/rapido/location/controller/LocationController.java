package com.rapido.location.controller;

import com.rapido.location.dto.ApiResponse;
import com.rapido.location.dto.LocationResponse;
import com.rapido.location.dto.NearbyRiderResponse;
import com.rapido.location.dto.RiderLocationRequest;
import com.rapido.location.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    /**
     * Heartbeat endpoint. The rider app (or a device simulator in Phase 11
     * tests) posts fresh coordinates. In production this is called every
     * few seconds while the rider is ONLINE.
     */
    @PostMapping("/rider")
    public ResponseEntity<ApiResponse<Void>> updateLocation(@Valid @RequestBody RiderLocationRequest request) {
        service.updateRiderLocation(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider location updated", null));
    }

    @GetMapping("/rider/{riderId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocation(@PathVariable Long riderId) {
        LocationResponse response = service.getRiderLocation(riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider location retrieved", response));
    }

    /**
     * Public radius search: riders within radiusKm of (lat, lon) who are
     * currently ONLINE, nearest first.
     */
    @GetMapping("/nearby/{lat}/{lon}")
    public ResponseEntity<ApiResponse<List<NearbyRiderResponse>>> findNearby(
            @PathVariable Double lat,
            @PathVariable Double lon,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        List<NearbyRiderResponse> response = service.findNearbyRiders(lat, lon, radiusKm);
        return ResponseEntity.ok(new ApiResponse<>(true, "Nearby available riders retrieved", response));
    }

    /**
     * Internal dispatch endpoint used by the Booking Service.
     * Returns bare rider IDs (nearest first) without the envelope wrapper,
     * which keeps consumer DTO parsing trivial for service-to-service calls.
     */
    @GetMapping("/internal/nearby/{lat}/{lon}")
    public ResponseEntity<List<Long>> findNearbyInternal(
            @PathVariable Double lat,
            @PathVariable Double lon,
            @RequestParam(defaultValue = "5.0") Double radiusKm) {
        return ResponseEntity.ok(service.findNearbyRiderIds(lat, lon, radiusKm));
    }
}
