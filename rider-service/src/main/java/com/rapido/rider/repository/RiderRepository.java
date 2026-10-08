package com.rapido.rider.repository;

import com.rapido.rider.entity.Rider;
import com.rapido.rider.entity.RiderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiderRepository extends JpaRepository<Rider, Long> {
    Optional<Rider> findByPhone(String phone);
    Optional<Rider> findByEmail(String email);
    Optional<Rider> findByDrivingLicenseNumber(String drivingLicenseNumber);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByDrivingLicenseNumber(String drivingLicenseNumber);

    @Modifying
    @Query("UPDATE Rider r SET r.status = :newStatus WHERE r.riderId = :riderId AND r.status = :expectedStatus")
    int updateStatusIfCurrent(@Param("riderId") Long riderId,
                              @Param("expectedStatus") RiderStatus expectedStatus,
                              @Param("newStatus") RiderStatus newStatus);

    @Query("SELECT r FROM Rider r JOIN r.vehicle v WHERE r.status = :status")
    List<Rider> findByStatusWithVehicle(RiderStatus status);
}
