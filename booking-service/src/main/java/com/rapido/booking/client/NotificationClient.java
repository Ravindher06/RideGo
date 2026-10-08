package com.rapido.booking.client;

import com.rapido.booking.dto.NotificationPayload;
import com.rapido.booking.entity.Booking;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Alert fan-out. Every method is fire-and-forget: an unavailable
 * Notification Service must never block or roll back a ride state
 * transition. In a later phase these calls become outbound queue events
 * without changing Booking's code paths.
 */
@Component
public class NotificationClient {

    private final RestClient restClient;

    public NotificationClient(@Qualifier("notificationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public void rideRequested(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDE_REQUESTED", "Ride requested",
                "Searching for a nearby rider from " + booking.getPickupAddress()
                        + " to " + booking.getDropAddress() + ".", booking.getBookingId(), booking.getFare()));
    }

    public void rideAccepted(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDE_ACCEPTED", "Rider accepted your ride",
                "Your rider has accepted the ride. Pickup OTP: " + booking.getOtp() + ".",
                booking.getBookingId(), booking.getFare()));
    }

    public void riderArriving(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDER_ARRIVING", "Rider is arriving",
                "Your rider has reached the pickup point.", booking.getBookingId(), booking.getFare()));
    }

    public void rideStarted(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDE_STARTED", "Ride started",
                "OTP verified. Your ride has begun. Fare: " + booking.getFare() + " INR.",
                booking.getBookingId(), booking.getFare()));
    }

    public void rideCompleted(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDE_COMPLETED", "Ride completed",
                "Thank you for riding with Rapido. Total fare: " + booking.getFare() + " INR.",
                booking.getBookingId(), booking.getFare()));
        notifySafely(rider(booking.getRiderId(), "RIDE_COMPLETED", "Ride completed",
                "The ride for booking " + booking.getBookingId() + " is complete.",
                booking.getBookingId(), booking.getFare()));
    }

    public void rideCancelled(Booking booking) {
        notifySafely(customer(booking.getCustomerId(), "RIDE_CANCELLED", "Ride cancelled",
                "Your ride booking " + booking.getBookingId() + " was cancelled.",
                booking.getBookingId(), booking.getFare()));
    }

    public void paymentSuccess(Booking booking, String referenceId) {
        notifySafely(customer(booking.getCustomerId(), "PAYMENT_SUCCESS", "Payment successful",
                "Payment of " + booking.getFare() + " INR for booking " + booking.getBookingId()
                        + " was captured (ref: " + referenceId + ").", booking.getBookingId(), booking.getFare()));
    }

    public void paymentFailed(Booking booking, String reason) {
        notifySafely(customer(booking.getCustomerId(), "PAYMENT_FAILED", "Payment failed",
                "Payment for booking " + booking.getBookingId() + " failed: " + reason
                        + ". Please pay from the app to avoid fare adjustment.", booking.getBookingId(), booking.getFare()));
    }

    private void notifySafely(NotificationPayload payload) {
        if (payload == null) {
            return;
        }
        try {
            restClient.post()
                    .uri("/notifications/internal/send")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // Fire-and-forget: alerts must not break the ride lifecycle.
        }
    }

    private NotificationPayload customer(Long customerId, String type, String title, String message,
                                         Long bookingId, BigDecimal fare) {
        return new NotificationPayload("CUSTOMER", customerId, type, title, message, bookingId, "APP", fare);
    }

    private NotificationPayload rider(Long riderId, String type, String title, String message,
                                      Long bookingId, BigDecimal fare) {
        if (riderId == null) {
            return null;
        }
        return new NotificationPayload("RIDER", riderId, type, title, message, bookingId, "APP", fare);
    }
}
