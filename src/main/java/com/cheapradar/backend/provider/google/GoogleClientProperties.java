package com.cheapradar.backend.provider.google;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.client.google")
public class GoogleClientProperties {
    private Integer maximumRequests;
    private Integer maximumTickets;
    private String provider;
    private String endpoint;
    private String apiKey;
}
