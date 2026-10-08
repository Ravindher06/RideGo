package com.rapido.notification.controller;

import com.rapido.notification.dto.ApiResponse;
import com.rapido.notification.dto.NotificationRequest;
import com.rapido.notification.dto.NotificationResponse;
import com.rapido.notification.entity.RecipientType;
import com.rapido.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> send(@Valid @RequestBody NotificationRequest request) {
        NotificationResponse response = service.sendNotification(request);
        return new ResponseEntity<>(
                new ApiResponse<>(true, "Notification dispatched", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @PathVariable Long id,
            @RequestParam RecipientType recipientType,
            @RequestParam Long recipientId) {
        NotificationResponse response = service.getNotificationById(id, recipientType, recipientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification retrieved", response));
    }

    /**
     * In-app notification center for a customer or rider.
     */
    @GetMapping("/user/{recipientId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByUser(
            @PathVariable Long recipientId,
            @RequestParam RecipientType recipientType) {
        List<NotificationResponse> response = service.getNotificationsForRecipient(recipientType, recipientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getByBooking(
            @PathVariable Long bookingId,
            @RequestParam RecipientType recipientType,
            @RequestParam Long recipientId) {
        List<NotificationResponse> response = service.getBookingNotifications(bookingId, recipientType, recipientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Booking notifications retrieved", response));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long id,
            @RequestParam RecipientType recipientType,
            @RequestParam Long recipientId) {
        NotificationResponse response = service.markAsRead(id, recipientType, recipientId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", response));
    }

    /**
     * Internal fire endpoint used by other services. Kept without the
     * envelope wrapper so service-to-service callers can parse it directly.
     */
    @PostMapping("/internal/send")
    public ResponseEntity<NotificationResponse> sendInternal(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendNotification(request));
    }
}
