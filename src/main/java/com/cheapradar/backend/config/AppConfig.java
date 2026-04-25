package com.cheapradar.backend.config;

import com.cheapradar.backend.client.google.GoogleClientProperties;
import com.cheapradar.backend.client.google.GoogleSearchClient;
import com.cheapradar.backend.client.google.mapping.ClientSearchResponseMapper;
import com.cheapradar.backend.client.test.TestSearchClient;
import com.cheapradar.backend.client.SearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AppConfig {

    @Bean
    public Map<String, SearchClient> searchClients(GoogleClientProperties googleClientProperties,
                                                   ClientSearchResponseMapper clientSearchResponseMapper) {
        return Map.of("TEST", new TestSearchClient(),
                googleClientProperties.getProvider(), new GoogleSearchClient(googleClientProperties, clientSearchResponseMapper));
    }
}
