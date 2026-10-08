package com.rapido.booking.dto;

import java.math.BigDecimal;

/**
 * Consumer-local wire DTO for the Notification Service's internal endpoint.
 * Keeping it local preserves the rule that no shared entity library exists.
 */
public class NotificationPayload {
    private String recipientType;
    private Long recipientId;
    private String type;
    private String title;
    private String message;
    private Long bookingId;
    private String channel;
    private BigDecimal fare;

    public NotificationPayload() {}

    public NotificationPayload(String recipientType, Long recipientId, String type, String title,
                               String message, Long bookingId, String channel, BigDecimal fare) {
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.bookingId = bookingId;
        this.channel = channel;
        this.fare = fare;
    }

    // Getters and Setters
    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}
