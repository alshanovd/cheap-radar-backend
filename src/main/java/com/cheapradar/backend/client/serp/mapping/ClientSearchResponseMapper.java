package com.cheapradar.backend.client.serp.mapping;

import com.cheapradar.backend.client.dto.ClientSearchResponse;
import com.cheapradar.backend.client.serp.SerpClientProperties;
import com.cheapradar.backend.client.serp.model.SerpSearchResponse;
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
    private final SerpClientProperties properties;

    public ClientSearchResponse map(List<SerpSearchResponse> serpSearchResponses) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return ClientSearchResponse.builder()
            .tickets(
                serpSearchResponses.stream()
                    .flatMap(response -> {
                        String link = response.getSearchMetadata().getGoogleFlightsUrl();
                        return response.getOtherFlights().stream()
                            .map(otherFlights -> {
                                SerpSearchResponse.OtherFlights.Flight flight = otherFlights.getFlights().get(0);
                                SerpSearchResponse.OtherFlights.Flight.Airport airportFrom = flight.getDepartureAirport();
                                SerpSearchResponse.OtherFlights.Flight.Airport airportTo = flight.getArrivalAirport();
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
                    .toList()
            )
            .build();
    }
}
