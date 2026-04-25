package com.cheapradar.backend.client.serp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.client.serp")
public class SerpClientProperties {
    private String provider;
    private String endpoint;
    private String apiKey;
}
