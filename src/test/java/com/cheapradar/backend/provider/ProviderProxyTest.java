package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderAggregateResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderProxyTest {
    private final ProviderSearchRequest request = ProviderSearchRequest.builder()
            .airportFrom("SYD")
            .airportTo("MEL")
            .dateFrom(LocalDate.of(2026, 5, 1))
            .dateTo(LocalDate.of(2026, 5, 1))
            .build();

    @Test
    void normalizesRequestedProviderSlugs() {
        ProviderProxy proxy = new ProviderProxy(List.of(new StubProvider("google")));

        ProviderAggregateResult result = proxy.search(List.of("GOOGLE"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void fallsBackUnknownProviderToTestProvider() {
        ProviderProxy proxy = new ProviderProxy(List.of(new StubProvider("test")));

        ProviderAggregateResult result = proxy.search(List.of("unknown"), request);

        assertEquals(List.of("test"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("unknown"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void keepsSuccessfulProviderResultsWhenAnotherProviderFails() {
        ProviderProxy proxy = new ProviderProxy(List.of(
                new StubProvider("google"),
                new FailingProvider("test")
        ));

        ProviderAggregateResult result = proxy.search(List.of("google", "test"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertEquals(List.of("test"), result.getFailedProviders().stream().toList());
    }

    private static class StubProvider implements FlightProvider {
        private final String slug;

        private StubProvider(String slug) {
            this.slug = slug;
        }

        @Override
        public String slug() {
            return slug;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            return ProviderSearchResponse.builder()
                    .tickets(List.of(ProviderTicket.builder()
                            .provider(slug)
                            .airportFrom(request.getAirportFrom())
                            .airportTo(request.getAirportTo())
                            .date(LocalDateTime.of(2026, 5, 1, 12, 0))
                            .price(BigDecimal.TEN)
                            .build()))
                    .build();
        }
    }

    private static class FailingProvider implements FlightProvider {
        private final String slug;

        private FailingProvider(String slug) {
            this.slug = slug;
        }

        @Override
        public String slug() {
            return slug;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            throw new RuntimeException("provider unavailable");
        }
    }
}
