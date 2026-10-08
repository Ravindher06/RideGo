package com.rapido.rider.service;

import com.rapido.rider.dto.RiderRequest;
import com.rapido.rider.dto.RiderResponse;
import com.rapido.rider.dto.VehicleRequest;
import com.rapido.rider.dto.VehicleResponse;
import com.rapido.rider.entity.Rider;
import com.rapido.rider.entity.RiderStatus;
import com.rapido.rider.entity.Vehicle;
import com.rapido.rider.exception.DuplicateResourceException;
import com.rapido.rider.exception.RiderNotFoundException;
import com.rapido.rider.exception.VehicleNotFoundException;
import com.rapido.rider.repository.RiderRepository;
import com.rapido.rider.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RiderServiceImpl implements RiderService {

    private final RiderRepository riderRepository;
    private final VehicleRepository vehicleRepository;

    public RiderServiceImpl(RiderRepository riderRepository, VehicleRepository vehicleRepository) {
        this.riderRepository = riderRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public RiderResponse createRider(RiderRequest request) {
        if (riderRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Rider with phone " + request.getPhone() + " already exists");
        }
        if (riderRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Rider with email " + request.getEmail() + " already exists");
        }
        if (riderRepository.existsByDrivingLicenseNumber(request.getDrivingLicenseNumber())) {
            throw new DuplicateResourceException("Driving license " + request.getDrivingLicenseNumber() + " already registered");
        }

        Rider rider = new Rider();
        rider.setName(request.getName());
        rider.setPhone(request.getPhone());
        rider.setEmail(request.getEmail());
        rider.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        rider.setWalletBalance(request.getWalletBalance() != null ? request.getWalletBalance() : BigDecimal.ZERO);
        rider.setStatus(RiderStatus.OFFLINE);
        rider.setAverageRating(BigDecimal.valueOf(5.00));

        Rider saved = riderRepository.save(rider);
        return mapToRiderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderResponse getRiderById(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));
        return mapToRiderResponse(rider);
    }

    @Override
    public RiderResponse updateRider(Long riderId, RiderRequest request) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        riderRepository.findByPhone(request.getPhone()).ifPresent(existing -> {
            if (!existing.getRiderId().equals(riderId)) {
                throw new DuplicateResourceException("Phone number " + request.getPhone() + " is already taken");
            }
        });

        riderRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getRiderId().equals(riderId)) {
                throw new DuplicateResourceException("Email " + request.getEmail() + " is already taken");
            }
        });

        riderRepository.findByDrivingLicenseNumber(request.getDrivingLicenseNumber()).ifPresent(existing -> {
            if (!existing.getRiderId().equals(riderId)) {
                throw new DuplicateResourceException("Driving license " + request.getDrivingLicenseNumber() + " is already registered");
            }
        });

        rider.setName(request.getName());
        rider.setPhone(request.getPhone());
        rider.setEmail(request.getEmail());
        rider.setDrivingLicenseNumber(request.getDrivingLicenseNumber());
        if (request.getWalletBalance() != null) {
            rider.setWalletBalance(request.getWalletBalance());
        }

        Rider updated = riderRepository.save(rider);
        return mapToRiderResponse(updated);
    }

    @Override
    public void deleteRider(Long riderId) {
        if (!riderRepository.existsById(riderId)) {
            throw new RiderNotFoundException("Rider not found with ID " + riderId);
        }
        riderRepository.deleteById(riderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiderResponse> getAllRiders() {
        return riderRepository.findAll().stream()
                .map(this::mapToRiderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VehicleResponse registerVehicle(Long riderId, VehicleRequest request) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        if (rider.getVehicle() != null) {
            throw new DuplicateResourceException("Rider already has a vehicle registered");
        }

        if (vehicleRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new DuplicateResourceException("Vehicle number " + request.getVehicleNumber() + " already registered");
        }

        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Registration number " + request.getRegistrationNumber() + " already registered");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setRider(rider);
        vehicle.setVehicleId(riderId);
        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleBrand(request.getVehicleBrand());
        vehicle.setVehicleModel(request.getVehicleModel());
        vehicle.setVehicleColor(request.getVehicleColor());
        vehicle.setVehicleCapacity(request.getVehicleCapacity());
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setPollutionCertificate(request.getPollutionCertificate());

        rider.setVehicle(vehicle);
        riderRepository.save(rider);

        return mapToVehicleResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleByRiderId(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        Vehicle vehicle = rider.getVehicle();
        if (vehicle == null) {
            throw new VehicleNotFoundException("No vehicle registered for rider with ID " + riderId);
        }
        return mapToVehicleResponse(vehicle);
    }

    @Override
    public VehicleResponse updateVehicle(Long riderId, VehicleRequest request) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        Vehicle vehicle = rider.getVehicle();
        if (vehicle == null) {
            throw new VehicleNotFoundException("No vehicle registered for rider with ID " + riderId);
        }

        vehicleRepository.findByVehicleNumber(request.getVehicleNumber()).ifPresent(existing -> {
            if (!existing.getVehicleId().equals(riderId)) {
                throw new DuplicateResourceException("Vehicle number " + request.getVehicleNumber() + " is already registered");
            }
        });

        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setVehicleBrand(request.getVehicleBrand());
        vehicle.setVehicleModel(request.getVehicleModel());
        vehicle.setVehicleColor(request.getVehicleColor());
        vehicle.setVehicleCapacity(request.getVehicleCapacity());
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setInsuranceNumber(request.getInsuranceNumber());
        vehicle.setPollutionCertificate(request.getPollutionCertificate());

        vehicleRepository.save(vehicle);
        return mapToVehicleResponse(vehicle);
    }

    @Override
    public void deleteVehicle(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        if (rider.getVehicle() == null) {
            throw new VehicleNotFoundException("No vehicle registered to delete for rider " + riderId);
        }

        // Must go OFFLINE if deleting vehicle
        rider.setStatus(RiderStatus.OFFLINE);
        rider.setVehicle(null);
        riderRepository.save(rider);
    }

    @Override
    public RiderResponse updateStatus(Long riderId, RiderStatus status) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));

        if (status == RiderStatus.ONLINE && rider.getVehicle() == null) {
            throw new IllegalStateException("Rider cannot go ONLINE without registering a vehicle first");
        }

        rider.setStatus(status);
        Rider saved = riderRepository.save(rider);
        return mapToRiderResponse(saved);
    }

    @Override
    public boolean updateStatusIfCurrent(Long riderId, RiderStatus expectedStatus, RiderStatus newStatus) {
        if (newStatus == RiderStatus.ONLINE) {
            Rider rider = riderRepository.findById(riderId)
                    .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));
            if (rider.getVehicle() == null) {
                throw new IllegalStateException("Rider cannot go ONLINE without a registered vehicle");
            }
        }
        return riderRepository.updateStatusIfCurrent(riderId, expectedStatus, newStatus) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public RiderStatus getStatus(Long riderId) {
        Rider rider = riderRepository.findById(riderId)
                .orElseThrow(() -> new RiderNotFoundException("Rider not found with ID " + riderId));
        return rider.getStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiderResponse> getAvailableRiders() {
        return riderRepository.findByStatusWithVehicle(RiderStatus.ONLINE).stream()
                .map(this::mapToRiderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkRiderExists(Long riderId) {
        return riderRepository.existsById(riderId);
    }

    private RiderResponse mapToRiderResponse(Rider entity) {
        RiderResponse response = new RiderResponse();
        response.setRiderId(entity.getRiderId());
        response.setName(entity.getName());
        response.setPhone(entity.getPhone());
        response.setEmail(entity.getEmail());
        response.setDrivingLicenseNumber(entity.getDrivingLicenseNumber());
        response.setWalletBalance(entity.getWalletBalance());
        response.setStatus(entity.getStatus());
        response.setAverageRating(entity.getAverageRating());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private VehicleResponse mapToVehicleResponse(Vehicle entity) {
        VehicleResponse response = new VehicleResponse();
        response.setVehicleId(entity.getVehicleId());
        response.setVehicleNumber(entity.getVehicleNumber());
        response.setVehicleType(entity.getVehicleType());
        response.setVehicleBrand(entity.getVehicleBrand());
        response.setVehicleModel(entity.getVehicleModel());
        response.setVehicleColor(entity.getVehicleColor());
        response.setVehicleCapacity(entity.getVehicleCapacity());
        response.setRegistrationNumber(entity.getRegistrationNumber());
        response.setInsuranceNumber(entity.getInsuranceNumber());
        response.setPollutionCertificate(entity.getPollutionCertificate());
        return response;
    }
}
