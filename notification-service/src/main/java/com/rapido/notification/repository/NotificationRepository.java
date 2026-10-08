package com.rapido.notification.repository;

import com.rapido.notification.entity.Notification;
import com.rapido.notification.entity.NotificationType;
import com.rapido.notification.entity.RecipientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientTypeAndRecipientIdOrderByIdDesc(RecipientType recipientType, Long recipientId);
    List<Notification> findByBookingIdAndRecipientTypeAndRecipientIdOrderByIdDesc(Long bookingId, RecipientType recipientType, Long recipientId);
    List<Notification> findByTypeAndRecipientTypeAndRecipientIdOrderByIdDesc(NotificationType type, RecipientType recipientType, Long recipientId);
}
