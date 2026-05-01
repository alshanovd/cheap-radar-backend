package com.cheapradar.backend.provider.aviasales;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.client.aviasales")
public class AviasalesClientProperties {
    private Integer maximumRequests;
    private Integer maximumTickets;
    private String provider;
    private String endpoint;
    private String apiKey;
    private String searchBaseUrl;
}
