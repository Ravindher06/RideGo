package com.rapido.rider.controller;

import com.rapido.rider.dto.*;
import com.rapido.rider.entity.RiderStatus;
import com.rapido.rider.service.RiderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/riders")
public class RiderController {

    private final RiderService service;

    public RiderController(RiderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiderResponse>> create(@Valid @RequestBody RiderRequest request) {
        RiderResponse response = service.createRider(request);
        return new ResponseEntity<>(
                new ApiResponse<>(true, "Rider registered successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RiderResponse>> getById(@PathVariable Long id) {
        RiderResponse response = service.getRiderById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider profile retrieved", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RiderResponse>> update(@PathVariable Long id, @Valid @RequestBody RiderRequest request) {
        RiderResponse response = service.updateRider(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider profile updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteRider(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider profile deleted", null));
    }

    // Vehicle Management
    @PostMapping("/{riderId}/vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(@PathVariable Long riderId, @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = service.registerVehicle(riderId, request);
        return new ResponseEntity<>(
                new ApiResponse<>(true, "Vehicle registered successfully to rider", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{riderId}/vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable Long riderId) {
        VehicleResponse response = service.getVehicleByRiderId(riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle details retrieved", response));
    }

    @PutMapping("/{riderId}/vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(@PathVariable Long riderId, @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = service.updateVehicle(riderId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle details updated", response));
    }

    @DeleteMapping("/{riderId}/vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Long riderId) {
        service.deleteVehicle(riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle details deleted and rider went OFFLINE", null));
    }

    // Status management
    @PutMapping("/{id}/online")
    public ResponseEntity<ApiResponse<RiderResponse>> goOnline(@PathVariable Long id) {
        RiderResponse response = service.updateStatus(id, RiderStatus.ONLINE);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider is now ONLINE", response));
    }

    @PutMapping("/{id}/offline")
    public ResponseEntity<ApiResponse<RiderResponse>> goOffline(@PathVariable Long id) {
        RiderResponse response = service.updateStatus(id, RiderStatus.OFFLINE);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider is now OFFLINE", response));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RiderStatus>> getStatus(@PathVariable Long id) {
        RiderStatus status = service.getStatus(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider status retrieved", status));
    }

    // Internal service communications
    @GetMapping("/internal/available")
    public ResponseEntity<List<RiderResponse>> listAvailableInternal() {
        return ResponseEntity.ok(service.getAvailableRiders());
    }

    @PutMapping("/internal/{id}/status")
    public ResponseEntity<?> updateStatusInternal(@PathVariable Long id,
                                                   @RequestParam RiderStatus status,
                                                   @RequestParam(required = false) RiderStatus expectedStatus) {
        if (expectedStatus != null) {
            boolean updated = service.updateStatusIfCurrent(id, expectedStatus, status);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Rider status changed; reservation was not acquired");
            }
            return ResponseEntity.ok(service.getRiderById(id));
        }
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    @GetMapping("/internal/{id}/exists")
    public ResponseEntity<Boolean> existsInternal(@PathVariable Long id) {
        return ResponseEntity.ok(service.checkRiderExists(id));
    }
}
