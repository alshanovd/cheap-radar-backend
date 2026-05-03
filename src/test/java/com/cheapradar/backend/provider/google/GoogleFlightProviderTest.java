package com.cheapradar.backend.provider.google;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleFlightProviderTest {
    @Test
    void requestsCheapestFlightsInUsd() throws IOException {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] response = """
                    {
                      "search_metadata": {
                        "google_flights_url": "https://www.google.com/travel/flights"
                      },
                      "other_flights": []
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            GoogleClientProperties properties = properties(server);
            GoogleFlightProvider provider = new GoogleFlightProvider(
                    properties,
                    new GoogleSearchResponseMapper(properties)
            );
            ProviderSearchRequest request = ProviderSearchRequest.builder()
                    .airportFrom("SYD")
                    .airportTo("BKK")
                    .dateFrom(LocalDate.of(2026, 5, 3))
                    .dateTo(LocalDate.of(2026, 5, 3))
                    .build();

            provider.search(request);

            assertTrue(query.get().contains("currency=USD"));
            assertTrue(query.get().contains("sort_by=2"));
            assertTrue(query.get().contains("stops=0"));
            assertFalse(query.get().contains("currency=AUD"));
        } finally {
            server.stop(0);
        }
    }

    private GoogleClientProperties properties(HttpServer server) {
        GoogleClientProperties properties = new GoogleClientProperties();
        properties.setProvider("google");
        properties.setMaximumRequests(1);
        properties.setMaximumTickets(3);
        properties.setApiKey("test-key");
        properties.setEndpoint("http://localhost:" + server.getAddress().getPort());
        return properties;
    }
}
