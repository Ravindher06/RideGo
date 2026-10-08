package com.rapido.booking.controller;

import com.rapido.booking.dto.*;
import com.rapido.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    // Customer operations
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> requestRide(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = service.requestRide(request);
        return new ResponseEntity<>(
                new ApiResponse<>(true, "Ride requested successfully. Searching for nearby riders.", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getById(@PathVariable Long id) {
        BookingResponse response = service.getBookingById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Booking details retrieved", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getByCustomerId(@PathVariable Long customerId) {
        List<BookingResponse> response = service.getBookingsByCustomerId(customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer bookings retrieved", response));
    }

    @GetMapping("/customer/{customerId}/current")
    public ResponseEntity<ApiResponse<BookingResponse>> getCurrentActiveRide(@PathVariable Long customerId) {
        BookingResponse response = service.getCurrentActiveRideForCustomer(customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Active ride details retrieved", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(@PathVariable Long id) {
        BookingResponse response = service.cancelBooking(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ride booking cancelled", response));
    }

    // Rider operations
    @GetMapping("/rider/{riderId}/requests")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getRiderRequests(@PathVariable Long riderId) {
        List<BookingResponse> response = service.getRiderRequests(riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assigned ride requests retrieved", response));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> accept(@PathVariable Long id, @RequestParam Long riderId) {
        BookingResponse response = service.acceptBooking(id, riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ride request accepted by rider", response));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> reject(@PathVariable Long id, @RequestParam Long riderId) {
        BookingResponse response = service.rejectBooking(id, riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ride request rejected. Finding next candidate.", response));
    }

    @PutMapping("/{id}/arrived")
    public ResponseEntity<ApiResponse<BookingResponse>> arrived(@PathVariable Long id, @RequestParam Long riderId) {
        BookingResponse response = service.riderArrived(id, riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rider has arrived at the pickup point", response));
    }

    @PostMapping("/{id}/verify-otp")
    public ResponseEntity<ApiResponse<BookingResponse>> startRide(@PathVariable Long id, @Valid @RequestBody VerifyOtpRequest request) {
        BookingResponse response = service.verifyOtpAndStartRide(id, request.getRiderId(), request.getOtp());
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP verified successfully. Ride started.", response));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> complete(@PathVariable Long id, @RequestParam Long riderId) {
        BookingResponse response = service.completeRide(id, riderId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Ride completed successfully. Thank you for using Rapido.", response));
    }
}
