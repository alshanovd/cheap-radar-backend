package com.cheapradar.backend.provider.test;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFlightProviderTest {
    @Test
    void mapsFakeSourceOffersToProviderTickets() {
        TestFlightProvider provider = new TestFlightProvider(new FakeTestFlightSource());
        ProviderSearchRequest request = ProviderSearchRequest.builder()
                .airportFrom("SYD")
                .airportTo("MEL")
                .dateFrom(LocalDate.of(2026, 5, 1))
                .dateTo(LocalDate.of(2026, 5, 3))
                .build();

        ProviderSearchResponse response = provider.search(request);

        assertEquals(3, response.getTickets().size());
        assertEquals("test", response.getTickets().get(0).getProvider());
        assertEquals("SYD", response.getTickets().get(0).getAirportFrom());
        assertEquals("MEL", response.getTickets().get(0).getAirportTo());
        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 25), response.getTickets().get(0).getDate());
        assertEquals(BigDecimal.valueOf(10000, 2), response.getTickets().get(0).getPrice());
    }
}
