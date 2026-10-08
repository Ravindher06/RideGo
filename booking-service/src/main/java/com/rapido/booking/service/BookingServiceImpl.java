package com.rapido.booking.service;

import com.rapido.booking.client.CustomerClient;
import com.rapido.booking.client.LocationClient;
import com.rapido.booking.client.NotificationClient;
import com.rapido.booking.client.PaymentClient;
import com.rapido.booking.client.RiderClient;
import com.rapido.booking.dto.BookingRequest;
import com.rapido.booking.dto.BookingResponse;
import com.rapido.booking.dto.PaymentSummary;
import com.rapido.booking.entity.Booking;
import com.rapido.booking.entity.BookingStatus;
import com.rapido.booking.exception.BookingNotFoundException;
import com.rapido.booking.exception.InvalidBookingStatusException;
import com.rapido.booking.exception.InvalidOtpException;
import com.rapido.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final CustomerClient customerClient;
    private final RiderClient riderClient;
    private final LocationClient locationClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final double nearbyRadiusKm;
    private final boolean fallbackToAllOnline;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              CustomerClient customerClient,
                              RiderClient riderClient,
                              LocationClient locationClient,
                              PaymentClient paymentClient,
                              NotificationClient notificationClient,
                              @Value("${rapido.dispatch.nearby-radius-km}") double nearbyRadiusKm,
                              @Value("${rapido.dispatch.fallback-to-all-online}") boolean fallbackToAllOnline) {
        this.bookingRepository = bookingRepository;
        this.customerClient = customerClient;
        this.riderClient = riderClient;
        this.locationClient = locationClient;
        this.paymentClient = paymentClient;
        this.notificationClient = notificationClient;
        this.nearbyRadiusKm = nearbyRadiusKm;
        this.fallbackToAllOnline = fallbackToAllOnline;
    }

    // ------------------------------------------------------------------
    // Customer operations
    // ------------------------------------------------------------------

    @Override
    public BookingResponse requestRide(BookingRequest request) {
        // 1. Validate Customer exists via REST (never via direct DB access)
        boolean customerExists = customerClient.checkCustomerExists(request.getCustomerId());
        if (!customerExists) {
            throw new IllegalArgumentException("Customer not found with ID " + request.getCustomerId());
        }

        // 2. One active ride per customer at a time
        Optional<Booking> active = bookingRepository.findActiveBookingByCustomerId(
                request.getCustomerId(), BookingStatus.COMPLETED, BookingStatus.CANCELLED);
        if (active.isPresent()) {
            throw new InvalidBookingStatusException("Customer already has an active ride: Booking " + active.get().getBookingId());
        }

        // 3. Create the booking with estimated distance, time, fare and OTP
        Booking booking = new Booking();
        booking.setCustomerId(request.getCustomerId());
        booking.setPickupLatitude(request.getPickupLatitude());
        booking.setPickupLongitude(request.getPickupLongitude());
        booking.setPickupAddress(request.getPickupAddress());
        booking.setDropLatitude(request.getDropLatitude());
        booking.setDropLongitude(request.getDropLongitude());
        booking.setDropAddress(request.getDropAddress());

        double dx = request.getPickupLatitude() - request.getDropLatitude();
        double dy = request.getPickupLongitude() - request.getDropLongitude();
        double rawDistance = Math.sqrt(dx * dx + dy * dy) * 111.0; // Approx KM
        if (rawDistance < 1.0) {
            rawDistance = 1.2;
        }
        BigDecimal distance = BigDecimal.valueOf(rawDistance).setScale(2, BigDecimal.ROUND_HALF_UP);
        booking.setDistance(distance);
        booking.setEstimatedTime((int) Math.ceil(rawDistance * 2.5));

        BigDecimal fare = distance.multiply(BigDecimal.valueOf(15.00)).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (fare.compareTo(BigDecimal.valueOf(50.00)) < 0) {
            fare = BigDecimal.valueOf(50.00);
        }
        booking.setFare(fare);

        booking.setOtp(String.format("%04d", new Random().nextInt(10000)));
        booking.setBookingStatus(BookingStatus.SEARCHING);
        Booking saved = bookingRepository.save(booking);

        // 4. GEO-first dispatch with graceful degradation
        tryAssignRider(saved, Collections.emptySet());

        // 5. Fire-and-forget alert
        notificationClient.rideRequested(saved);

        return mapToResponse(saved);
    }

    /**
     * Candidate selection strategy:
     * 1. Ask the Location Service for ONLINE riders inside the pickup radius
     *    (Redis GEO, nearest first).
     * 2. If GEO returns nothing (cold start or Redis down), fall back to the
     *    Rider Service's flat ONLINE list.
     * 3. Reserve the first candidate by flipping it to BUSY in the Rider
     *    Service; the status flip is the reservation token.
     */
    private void tryAssignRider(Booking booking, Set<Long> excludedRiders) {
        List<Long> candidates = nearbyCandidates(booking, excludedRiders);

        if (candidates.isEmpty()) {
            booking.setRiderId(null);
            booking.setBookingStatus(BookingStatus.SEARCHING);
            bookingRepository.save(booking);
            log.warn("No candidates for booking {}; remaining in SEARCHING", booking.getBookingId());
            return;
        }

        for (Long candidateId : candidates) {
            try {
                // Atomic ONLINE -> BUSY transition prevents two bookings from
                // claiming the same rider concurrently.
                riderClient.reserveRider(candidateId);
                booking.setRiderId(candidateId);
                booking.setBookingStatus(BookingStatus.RIDER_ASSIGNED);
                bookingRepository.save(booking);
                return;
            } catch (Exception ex) {
                log.warn("Could not reserve rider {} for booking {}; trying next candidate",
                        candidateId, booking.getBookingId());
            }
        }

        booking.setRiderId(null);
        booking.setBookingStatus(BookingStatus.SEARCHING);
        bookingRepository.save(booking);
    }

    private List<Long> nearbyCandidates(Booking booking, Set<Long> excludedRiders) {
        Set<Long> excluded = new HashSet<>(excludedRiders);
        excluded.add(booking.getRiderId());

        List<Long> geoCandidates = locationClient.findNearbyRiderIds(
                booking.getPickupLatitude(), booking.getPickupLongitude(), nearbyRadiusKm);
        List<Long> filtered = geoCandidates.stream().filter(id -> !excluded.contains(id)).collect(Collectors.toList());
        if (!filtered.isEmpty()) {
            return filtered;
        }

        if (!fallbackToAllOnline) {
            return List.of();
        }

        return riderClient.getAvailableRiders().stream()
                .map(row -> (Number) row.get("riderId"))
                .filter(java.util.Objects::nonNull)
                .map(Number::longValue)
                .filter(id -> !excluded.contains(id))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID " + bookingId));
        return mapToResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(Long customerId) {
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getCurrentActiveRideForCustomer(Long customerId) {
        Booking booking = bookingRepository.findActiveBookingByCustomerId(
                customerId, BookingStatus.COMPLETED, BookingStatus.CANCELLED)
                .orElseThrow(() -> new BookingNotFoundException("No active booking found for customer " + customerId));
        return mapToResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID " + bookingId));

        BookingStatus current = booking.getBookingStatus();
        if (current == BookingStatus.STARTED || current == BookingStatus.COMPLETED || current == BookingStatus.CANCELLED) {
            throw new InvalidBookingStatusException("Cannot cancel booking in status " + current);
        }

        releaseRider(booking);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        Booking saved = bookingRepository.save(booking);
        notificationClient.rideCancelled(saved);
        return mapToResponse(saved);
    }

    // ------------------------------------------------------------------
    // Rider operations
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getRiderRequests(Long riderId) {
        return bookingRepository.findAssignedRequestsForRider(riderId, BookingStatus.RIDER_ASSIGNED).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse acceptBooking(Long bookingId, Long riderId) {
        Booking booking = requireAssigned(bookingId, riderId);
        if (booking.getBookingStatus() != BookingStatus.RIDER_ASSIGNED) {
            throw new InvalidBookingStatusException("Cannot accept booking in status " + booking.getBookingStatus());
        }

        booking.setBookingStatus(BookingStatus.RIDER_ACCEPTED);
        booking.setAcceptedAt(Instant.now());
        Booking saved = bookingRepository.save(booking);
        notificationClient.rideAccepted(saved);
        return mapToResponse(saved);
    }

    @Override
    public BookingResponse rejectBooking(Long bookingId, Long riderId) {
        Booking booking = requireAssigned(bookingId, riderId);
        if (booking.getBookingStatus() != BookingStatus.RIDER_ASSIGNED) {
            throw new InvalidBookingStatusException("Cannot reject booking in status " + booking.getBookingStatus());
        }

        // Release the rejecting rider, exclude them, and search again
        releaseRider(booking);
        Set<Long> excluded = new HashSet<>();
        excluded.add(riderId);

        booking.setRiderId(null);
        booking.setBookingStatus(BookingStatus.SEARCHING);
        Booking saved = bookingRepository.save(booking);

        tryAssignRider(saved, excluded);
        return mapToResponse(saved);
    }

    @Override
    public BookingResponse riderArrived(Long bookingId, Long riderId) {
        Booking booking = requireAssigned(bookingId, riderId);
        if (booking.getBookingStatus() != BookingStatus.RIDER_ACCEPTED) {
            throw new InvalidBookingStatusException("Rider can only arrive if ride is ACCEPTED");
        }

        booking.setBookingStatus(BookingStatus.RIDER_ARRIVED);
        Booking saved = bookingRepository.save(booking);
        notificationClient.riderArriving(saved);
        return mapToResponse(saved);
    }

    @Override
    public BookingResponse verifyOtpAndStartRide(Long bookingId, Long riderId, String otp) {
        Booking booking = requireAssigned(bookingId, riderId);
        if (booking.getBookingStatus() != BookingStatus.RIDER_ARRIVED) {
            throw new InvalidBookingStatusException("Ride can only start if rider has ARRIVED");
        }

        if (!booking.getOtp().equals(otp)) {
            throw new InvalidOtpException("Invalid OTP provided");
        }

        booking.setBookingStatus(BookingStatus.STARTED);
        booking.setStartedAt(Instant.now());
        Booking saved = bookingRepository.save(booking);
        notificationClient.rideStarted(saved);
        return mapToResponse(saved);
    }

    @Override
    public BookingResponse completeRide(Long bookingId, Long riderId) {
        Booking booking = requireAssigned(bookingId, riderId);
        if (booking.getBookingStatus() != BookingStatus.STARTED) {
            throw new InvalidBookingStatusException("Ride can only complete if status is STARTED");
        }

        // 1. Release the rider back to ONLINE
        releaseRider(booking);

        // 2. Complete the booking
        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());
        Booking saved = bookingRepository.save(booking);

        notificationClient.rideCompleted(saved);

        // 3. Settle the fare via the Payment Service. The idempotency key
        //    ("booking-" + id) means retries after a timeout can never
        //    double-charge. Failure here does not block completion; the
        //    unpaid ride is surfaced via a PAYMENT_FAILED alert and can be
        //    re-settled or reconciled.
        PaymentSummary payment = paymentClient.settleRide(saved.getBookingId(), saved.getCustomerId(), saved.getFare());
        if (payment != null && "SUCCESS".equals(payment.getPaymentStatus())) {
            log.info("Booking {} settled: payment {} ref {}",
                    saved.getBookingId(), payment.getPaymentId(), payment.getReferenceId());
            notificationClient.paymentSuccess(saved, payment.getReferenceId());
        } else {
            log.error("Settlement failed for booking {}; will require reconciliation", saved.getBookingId());
            notificationClient.paymentFailed(saved,
                    payment != null ? payment.getFailureReason() : "Payment service unreachable");
        }

        return mapToResponse(saved);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Booking requireAssigned(Long bookingId, Long riderId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID " + bookingId));
        if (booking.getRiderId() == null || !riderId.equals(booking.getRiderId())) {
            throw new IllegalArgumentException("Rider " + riderId + " is not assigned to booking " + bookingId);
        }
        return booking;
    }

    private void releaseRider(Booking booking) {
        if (booking.getRiderId() != null) {
            try {
                riderClient.releaseRider(booking.getRiderId());
            } catch (Exception ex) {
                // Compensating action failed: log for reconciliation. The
                // rider stays BUSY at worst; an ops sweep can re-release.
                log.error("Failed to release rider {} for booking {}", booking.getRiderId(), booking.getBookingId(), ex);
            }
        }
    }

    private BookingResponse mapToResponse(Booking entity) {
        BookingResponse r = new BookingResponse();
        r.setBookingId(entity.getBookingId());
        r.setCustomerId(entity.getCustomerId());
        r.setRiderId(entity.getRiderId());
        r.setPickupLatitude(entity.getPickupLatitude());
        r.setPickupLongitude(entity.getPickupLongitude());
        r.setPickupAddress(entity.getPickupAddress());
        r.setDropLatitude(entity.getDropLatitude());
        r.setDropLongitude(entity.getDropLongitude());
        r.setDropAddress(entity.getDropAddress());
        r.setDistance(entity.getDistance());
        r.setEstimatedTime(entity.getEstimatedTime());
        r.setFare(entity.getFare());
        r.setOtp(entity.getOtp());
        r.setBookingStatus(entity.getBookingStatus());
        r.setCreatedAt(entity.getCreatedAt());
        r.setAcceptedAt(entity.getAcceptedAt());
        r.setStartedAt(entity.getStartedAt());
        r.setCompletedAt(entity.getCompletedAt());
        r.setCancelledAt(entity.getCancelledAt());
        return r;
    }
}
