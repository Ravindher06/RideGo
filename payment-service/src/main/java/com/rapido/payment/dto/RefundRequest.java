package com.rapido.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class RefundRequest {
    @NotBlank(message = "Reason is required for audit trail")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
