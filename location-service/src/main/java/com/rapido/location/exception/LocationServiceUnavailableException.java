package com.rapido.location.exception;

public class LocationServiceUnavailableException extends RuntimeException {
    public LocationServiceUnavailableException(String message) {
        super(message);
    }
}
