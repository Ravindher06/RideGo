package com.rapido.booking.service;

import com.rapido.booking.dto.BookingRequest;
import com.rapido.booking.dto.BookingResponse;

import java.util.List;

public interface BookingService {
    // Customer operations
    BookingResponse requestRide(BookingRequest request);
    BookingResponse getBookingById(Long bookingId);
    List<BookingResponse> getBookingsByCustomerId(Long customerId);
    BookingResponse getCurrentActiveRideForCustomer(Long customerId);
    BookingResponse cancelBooking(Long bookingId);

    // Rider operations
    List<BookingResponse> getRiderRequests(Long riderId);
    BookingResponse acceptBooking(Long bookingId, Long riderId);
    BookingResponse rejectBooking(Long bookingId, Long riderId);
    BookingResponse riderArrived(Long bookingId, Long riderId);
    BookingResponse verifyOtpAndStartRide(Long bookingId, Long riderId, String otp);
    BookingResponse completeRide(Long bookingId, Long riderId);
}
