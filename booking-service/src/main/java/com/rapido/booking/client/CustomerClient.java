package com.rapido.booking.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(@Qualifier("customerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public boolean checkCustomerExists(Long customerId) {
        try {
            Boolean exists = restClient.get()
                    .uri("/customers/internal/{id}/exists", customerId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        // Suppress 404 as false
                    })
                    .body(Boolean.class);
            return exists != null && exists;
        } catch (Exception ex) {
            // Treat integration network/port issues as 503 dependencies
            throw new RuntimeException("Customer service is currently unreachable: " + ex.getMessage());
        }
    }
}
