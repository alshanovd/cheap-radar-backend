package com.cheapradar.backend.provider.google;

import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import com.cheapradar.backend.provider.google.model.GoogleSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class GoogleSearchResponseMapper {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private final GoogleClientProperties properties;

    public ProviderSearchResponse map(List<GoogleSearchResponse> googleSearchResponse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        String provider = properties.getProvider().toLowerCase(Locale.ROOT);

        return ProviderSearchResponse.builder()
                .tickets(googleSearchResponse.stream()
                        .flatMap(response -> {
                            String link = response.getSearchMetadata().getGoogleFlightsUrl();
                            return response.getOtherFlights().stream()
                                    .map(otherFlights -> {
                                        GoogleSearchResponse.OtherFlights.Flight flight = otherFlights.getFlights().get(0);
                                        GoogleSearchResponse.OtherFlights.Flight.Airport airportFrom = flight.getDepartureAirport();
                                        GoogleSearchResponse.OtherFlights.Flight.Airport airportTo = flight.getArrivalAirport();
                                        return ProviderTicket.builder()
                                                .link(link)
                                                .price(otherFlights.getPrice())
                                                .airportFrom(airportFrom.getId())
                                                .airportTo(airportTo.getId())
                                                .date(LocalDateTime.parse(airportFrom.getTime(), formatter))
                                                .provider(provider)
                                                .airline(flight.getAirline())
                                                .airlineLogo(flight.getAirlineLogo())
                                                .build();
                                    });
                        })
                        .sorted()
                        .limit(properties.getMaximumTickets())
                        .toList())
                .build();
    }
}
