package com.cheapradar.backend.client.google.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoogleSearchResponse {
    @JsonProperty("search_metadata")
    private SearchMetadata searchMetadata;

    @JsonProperty("other_flights")
    private List<OtherFlights> otherFlights;

    @Data
    public static class SearchMetadata {
        @JsonProperty("google_flights_url")
        private String googleFlightsUrl;
    }

    @Data
    public static class OtherFlights {
        private List<Flight> flights;
        private BigDecimal price;

        @Data
        public static class Flight {
            @JsonProperty("departure_airport")
            private Airport departureAirport;

            @JsonProperty("arrival_airport")
            private Airport arrivalAirport;

            private String airline;

            @JsonProperty("airline_logo")
            private String airlineLogo;

            @Data
            public static class Airport {
                private String id;
                private String time;
            }
        }
    }
}
