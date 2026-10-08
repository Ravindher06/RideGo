package com.rapido.notification.service;

import com.rapido.notification.dto.NotificationRequest;
import com.rapido.notification.dto.NotificationResponse;
import com.rapido.notification.entity.Notification;
import com.rapido.notification.entity.RecipientType;
import com.rapido.notification.exception.NotificationNotFoundException;
import com.rapido.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delivery today is a pluggable stub: it persists the record and simulates
 * sending on the requested channel (console delivery). Swapping in real
 * SMS/push/email adapters — or an outbound message queue for at-least-once
 * delivery — does not change any calling service.
 */
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository repository;
    private final boolean consoleDelivery;

    public NotificationServiceImpl(NotificationRepository repository,
                                   @Value("${rapido.notification.console-delivery}") boolean consoleDelivery) {
        this.repository = repository;
        this.consoleDelivery = consoleDelivery;
    }

    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setRecipientType(request.getRecipientType());
        notification.setRecipientId(request.getRecipientId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setBookingId(request.getBookingId());
        notification.setChannel(request.getChannel() != null ? request.getChannel().toUpperCase() : "APP");

        Notification saved = repository.save(notification);

        deliver(saved);
        return mapToResponse(saved);
    }

    /**
     * Channel adapter stub. Production: route to push (FCM/APNS), SMS
     * (MSG91/Twilio), or email based on the channel field.
     */
    private void deliver(Notification notification) {
        notification.setDelivered(true);
        notification.setDeliveredAt(Instant.now());
        repository.save(notification);

        if (consoleDelivery) {
            log.info("[{}] -> {} {} ({}): {} — {}",
                    notification.getChannel(),
                    notification.getRecipientType(),
                    notification.getRecipientId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId, RecipientType recipientType, Long recipientId) {
        Notification notification = repository.findById(notificationId)
                .filter(n -> n.getRecipientType() == recipientType && n.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found for this recipient"));
        return mapToResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForRecipient(RecipientType recipientType, Long recipientId) {
        return repository.findByRecipientTypeAndRecipientIdOrderByIdDesc(recipientType, recipientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getBookingNotifications(Long bookingId, RecipientType recipientType, Long recipientId) {
        return repository.findByBookingIdAndRecipientTypeAndRecipientIdOrderByIdDesc(bookingId, recipientType, recipientId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId, RecipientType recipientType, Long recipientId) {
        Notification notification = repository.findById(notificationId)
                .filter(n -> n.getRecipientType() == recipientType && n.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found for this recipient"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = repository.save(notification);
        }
        return mapToResponse(notification);
    }

    private NotificationResponse mapToResponse(Notification entity) {
        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(entity.getNotificationId());
        response.setRecipientType(entity.getRecipientType());
        response.setRecipientId(entity.getRecipientId());
        response.setType(entity.getType());
        response.setTitle(entity.getTitle());
        response.setMessage(entity.getMessage());
        response.setBookingId(entity.getBookingId());
        response.setChannel(entity.getChannel());
        response.setDelivered(entity.isDelivered());
        response.setRead(entity.getReadAt() != null);
        response.setDeliveredAt(entity.getDeliveredAt());
        response.setReadAt(entity.getReadAt());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
