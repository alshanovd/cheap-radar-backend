package com.cheapradar.backend.service;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.mail")
public class EmailProperties {
    private String sender;
    private String lookupsUrl;
}
