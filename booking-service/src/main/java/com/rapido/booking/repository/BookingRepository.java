package com.rapido.booking.repository;

import com.rapido.booking.entity.Booking;
import com.rapido.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByRiderId(Long riderId);

    @Query("SELECT b FROM Booking b WHERE b.customerId = :customerId AND b.bookingStatus NOT IN (:completed, :cancelled)")
    Optional<Booking> findActiveBookingByCustomerId(Long customerId, BookingStatus completed, BookingStatus cancelled);

    @Query("SELECT b FROM Booking b WHERE b.riderId = :riderId AND b.bookingStatus = :status")
    List<Booking> findAssignedRequestsForRider(Long riderId, BookingStatus status);
}
