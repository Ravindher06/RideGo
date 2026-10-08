package com.rapido.booking.client;

import com.rapido.booking.dto.ApiResponse;
import com.rapido.booking.dto.PaymentSummary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;

/**
 * Settlement integration. The Booking Service never stores payment rows;
 * it triggers the charge with a stable idempotency key and only surfaces
 * the payment reference/status on the booking response.
 */
@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(@Qualifier("paymentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Best-effort settlement call. Failures are returned to the caller as a
     * null summary so the ride can still be COMPLETED; the payment is then
     * reconciled offline or re-triggered (the idempotency key guarantees no
     * double charge on retry).
     */
    public PaymentSummary settleRide(Long bookingId, Long customerId, BigDecimal amount) {
        try {
            ApiResponse<PaymentSummary> response = restClient.post()
                    .uri("/payments")
                    .body(new SettlementPayload(bookingId, customerId, amount, "WALLET", "booking-" + bookingId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<PaymentSummary>>() {});
            return response != null ? response.getData() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    public PaymentSummary getPaymentByBooking(Long bookingId) {
        try {
            return restClient.get()
                    .uri("/payments/internal/booking/{bookingId}/status", bookingId)
                    .retrieve()
                    .body(PaymentSummary.class);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Consumer-local wire DTO for the Payment Service creation endpoint. */
    public record SettlementPayload(
            Long bookingId,
            Long customerId,
            BigDecimal amount,
            String paymentMethod,
            String idempotencyKey) {}
}
