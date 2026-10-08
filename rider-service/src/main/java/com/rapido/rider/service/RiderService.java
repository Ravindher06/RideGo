package com.rapido.rider.service;

import com.rapido.rider.dto.RiderRequest;
import com.rapido.rider.dto.RiderResponse;
import com.rapido.rider.dto.VehicleRequest;
import com.rapido.rider.dto.VehicleResponse;
import com.rapido.rider.entity.RiderStatus;

import java.util.List;

public interface RiderService {
    RiderResponse createRider(RiderRequest request);
    RiderResponse getRiderById(Long riderId);
    RiderResponse updateRider(Long riderId, RiderRequest request);
    void deleteRider(Long riderId);
    List<RiderResponse> getAllRiders();

    VehicleResponse registerVehicle(Long riderId, VehicleRequest request);
    VehicleResponse getVehicleByRiderId(Long riderId);
    VehicleResponse updateVehicle(Long riderId, VehicleRequest request);
    void deleteVehicle(Long riderId);

    RiderResponse updateStatus(Long riderId, RiderStatus status);

    boolean updateStatusIfCurrent(Long riderId, RiderStatus expectedStatus, RiderStatus newStatus);
    RiderStatus getStatus(Long riderId);

    List<RiderResponse> getAvailableRiders();
    boolean checkRiderExists(Long riderId);
}
