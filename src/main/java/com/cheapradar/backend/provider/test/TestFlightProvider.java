package com.cheapradar.backend.provider.test;

import com.cheapradar.backend.provider.FlightProvider;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class TestFlightProvider implements FlightProvider {
    public static final String SLUG = "test";

    private final FakeTestFlightSource fakeTestFlightSource;

    @Override
    public String slug() {
        return SLUG;
    }

    @Override
    public ProviderSearchResponse search(ProviderSearchRequest request) {
        return ProviderSearchResponse.builder()
                .tickets(fakeTestFlightSource.search(request).stream()
                        .map(this::map)
                        .toList())
                .build();
    }

    private ProviderTicket map(TestFlightOffer offer) {
        return ProviderTicket.builder()
                .provider(SLUG)
                .airportFrom(offer.getOriginCode())
                .airportTo(offer.getDestinationCode())
                .date(LocalDateTime.ofInstant(Instant.ofEpochSecond(offer.getDepartureEpochSeconds()), ZoneOffset.UTC))
                .link(offer.getBookingUri())
                .price(BigDecimal.valueOf(offer.getCostCents(), 2))
                .airline(offer.getCarrierName())
                .airlineLogo(offer.getCarrierImageUri())
                .build();
    }
}
