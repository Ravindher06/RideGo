package com.rapido.payment.controller;

import com.rapido.payment.dto.ApiResponse;
import com.rapido.payment.dto.PaymentRequest;
import com.rapido.payment.dto.PaymentResponse;
import com.rapido.payment.dto.RefundRequest;
import com.rapido.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    /**
     * Idempotent: repeat the same idempotencyKey after a network timeout and
     * the original payment is returned; the customer is never charged twice.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = service.createPayment(request);
        HttpStatus status = response.getPaymentStatus().name().equals("SUCCESS") ? HttpStatus.CREATED : HttpStatus.OK;
        String message = response.getPaymentStatus().name().equals("SUCCESS")
                ? "Payment captured successfully"
                : "Payment attempt failed: " + response.getFailureReason();
        return new ResponseEntity<>(new ApiResponse<>(response.getPaymentStatus().name().equals("SUCCESS"), message, response), status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable Long id) {
        PaymentResponse response = service.getPaymentById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment details retrieved", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByBookingId(@PathVariable Long bookingId) {
        PaymentResponse response = service.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment for booking retrieved", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByCustomerId(@PathVariable Long customerId) {
        List<PaymentResponse> response = service.getPaymentsByCustomerId(customerId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Customer payments retrieved", response));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(@PathVariable Long id, @Valid @RequestBody(required = false) RefundRequest request) {
        RefundRequest body = (request != null) ? request : new RefundRequest();
        if (body.getReason() == null || body.getReason().isBlank()) {
            body.setReason("No reason provided");
        }
        PaymentResponse response = service.refundPayment(id, body);
        return ResponseEntity.ok(new ApiResponse<>(true, "Payment refunded successfully", response));
    }

    /**
     * Internal status lookup used by the Booking Service to surface the
     * settlement result on the booking page without storing payment data.
     */
    @GetMapping("/internal/booking/{bookingId}/status")
    public ResponseEntity<PaymentResponse> getStatusByBookingInternal(@PathVariable Long bookingId) {
        return ResponseEntity.ok(service.getPaymentByBookingId(bookingId));
    }
}
