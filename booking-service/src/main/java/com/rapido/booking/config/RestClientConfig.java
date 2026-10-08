package com.rapido.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${rapido.services.customer-url}")
    private String customerServiceUrl;

    @Value("${rapido.services.rider-url}")
    private String riderServiceUrl;

    @Value("${rapido.services.location-url}")
    private String locationServiceUrl;

    @Value("${rapido.services.payment-url}")
    private String paymentServiceUrl;

    @Value("${rapido.services.notification-url}")
    private String notificationServiceUrl;

    @Bean
    public RestClient customerRestClient() {
        return RestClient.builder()
                .baseUrl(customerServiceUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    @Bean
    public RestClient riderRestClient() {
        return RestClient.builder()
                .baseUrl(riderServiceUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    @Bean
    public RestClient locationRestClient() {
        return RestClient.builder()
                .baseUrl(locationServiceUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    @Bean
    public RestClient paymentRestClient() {
        return RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .requestFactory(settlementFactory())
                .build();
    }

    @Bean
    public RestClient notificationRestClient() {
        return RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .requestFactory(timeoutFactory())
                .build();
    }

    /** 3s for lifecycle-critical reads: bounded so a stalled peer cannot pin a thread. */
    private SimpleClientHttpRequestFactory timeoutFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        return factory;
    }

    /** Settlement may run gateway retries; allow a longer read window. */
    private SimpleClientHttpRequestFactory settlementFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        return factory;
    }
}
