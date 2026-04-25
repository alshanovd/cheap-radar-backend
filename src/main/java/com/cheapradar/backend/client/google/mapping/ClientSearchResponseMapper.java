package com.cheapradar.backend.client.google.mapping;

import com.cheapradar.backend.client.dto.ClientSearchResponse;
import com.cheapradar.backend.client.google.GoogleClientProperties;
import com.cheapradar.backend.client.google.model.GoogleSearchResponse;
import com.cheapradar.backend.dto.search.TicketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClientSearchResponseMapper {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private final GoogleClientProperties properties;

    public ClientSearchResponse map(List<GoogleSearchResponse> googleSearchResponse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return ClientSearchResponse.builder()
            .tickets(
                googleSearchResponse.stream()
                    .flatMap(response -> {
                        String link = response.getSearchMetadata().getGoogleFlightsUrl();
                        return response.getOtherFlights().stream()
                            .map(otherFlights -> {
                                GoogleSearchResponse.OtherFlights.Flight flight = otherFlights.getFlights().get(0);
                                GoogleSearchResponse.OtherFlights.Flight.Airport airportFrom = flight.getDepartureAirport();
                                GoogleSearchResponse.OtherFlights.Flight.Airport airportTo = flight.getArrivalAirport();
                                return TicketResponse.builder()
                                    .link(link)
                                    .price(otherFlights.getPrice())
                                    .airportFrom(airportFrom.getId())
                                    .airportTo(airportTo.getId())
                                    .date(LocalDateTime.parse(airportFrom.getTime(), formatter))
                                    .provider(properties.getProvider())
                                    .build();
                            });
                    })
                    .sorted()
                    .limit(properties.getMaximumTickets())
                    .toList()
            )
            .build();
    }
}
