package com.rapido.notification.service;

import com.rapido.notification.dto.NotificationRequest;
import com.rapido.notification.dto.NotificationResponse;
import com.rapido.notification.entity.RecipientType;

import java.util.List;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long notificationId, RecipientType recipientType, Long recipientId);

    List<NotificationResponse> getNotificationsForRecipient(RecipientType recipientType, Long recipientId);

    List<NotificationResponse> getBookingNotifications(Long bookingId, RecipientType recipientType, Long recipientId);

    NotificationResponse markAsRead(Long notificationId, RecipientType recipientType, Long recipientId);
}
