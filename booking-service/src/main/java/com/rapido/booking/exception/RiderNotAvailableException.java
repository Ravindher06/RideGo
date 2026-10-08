package com.rapido.booking.exception;

public class RiderNotAvailableException extends RuntimeException {
    public RiderNotAvailableException(String message) {
        super(message);
    }
}
