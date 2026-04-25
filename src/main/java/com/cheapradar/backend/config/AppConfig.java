package com.cheapradar.backend.config;

import com.cheapradar.backend.client.serp.SerpClientProperties;
import com.cheapradar.backend.client.serp.SerpSearchClient;
import com.cheapradar.backend.client.serp.mapping.ClientSearchResponseMapper;
import com.cheapradar.backend.client.test.TestSearchClient;
import com.cheapradar.backend.client.SearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AppConfig {

    @Bean
    public Map<String, SearchClient> searchClients(SerpClientProperties serpClientProperties,
                                                   ClientSearchResponseMapper clientSearchResponseMapper) {
        return Map.of("TEST", new TestSearchClient(),
                serpClientProperties.getProvider(), new SerpSearchClient(serpClientProperties, clientSearchResponseMapper));
    }
}
