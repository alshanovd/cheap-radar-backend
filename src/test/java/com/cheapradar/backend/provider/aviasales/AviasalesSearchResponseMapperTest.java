package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.aviasales.AviasalesSearchResponseMapper.AviasalesHtmlResponse;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AviasalesSearchResponseMapperTest {
    private static final String SEARCH_URL = "https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0";

    @Test
    void mapsTicketFromPriceAnchor() {
        AviasalesSearchResponseMapper mapper = new AviasalesSearchResponseMapper(properties());

        ProviderSearchResponse response = mapper.map(List.of(new AviasalesHtmlResponse(
                ticketHtml("data-test-id=\"ticket-preview\"", "data-test-id=\"price\""),
                SEARCH_URL,
                LocalDate.of(2026, 5, 3)
        )), request());

        ProviderTicket ticket = response.getTickets().get(0);
        assertEquals(1, response.getTickets().size());
        assertEquals("aviasales", ticket.getProvider());
        assertEquals("SYD", ticket.getAirportFrom());
        assertEquals("BKK", ticket.getAirportTo());
        assertEquals(LocalDateTime.of(2026, 5, 3, 13, 0), ticket.getDate());
        assertEquals(new BigDecimal("336"), ticket.getPrice());
        assertEquals("Scoot", ticket.getAirline());
        assertEquals("https://img.avs.io/pics/al_square/TR@avif?rs=fit:120:120", ticket.getAirlineLogo());
        assertEquals(SEARCH_URL, ticket.getLink());
    }

    @Test
    void handlesEscapedPriceAttributes() {
        AviasalesSearchResponseMapper mapper = new AviasalesSearchResponseMapper(properties());

        ProviderSearchResponse response = mapper.map(List.of(new AviasalesHtmlResponse(
                ticketHtml("data-test-id=\\\"ticket-preview\\\"", "data-test-id=\\\"price\\\""),
                SEARCH_URL,
                LocalDate.of(2026, 5, 3)
        )), request());

        assertEquals(1, response.getTickets().size());
        assertEquals(new BigDecimal("336"), response.getTickets().get(0).getPrice());
    }

    private String ticketHtml(String ticketAttribute, String priceAttribute) {
        return """
                <div %s>
                    <div %s>336\u202F$</div>
                    <img alt="Scoot" src="//img.avs.io/pics/al_square/TR@avif?rs=fit:120:120">
                    <div data-test-id="text">13:00</div>
                    <div data-test-id="text">Сидней</div>
                    <div data-test-id="text">3&nbsp;мая,&nbsp;вс</div>
                    <span data-test-id="text">SYD</span>
                    <span data-test-id="text">1 д 4 ч 25 м в пути, 1 пересадка</span>
                    <span data-test-id="text">SIN</span>
                    <span data-test-id="text">BKK</span>
                    <div data-test-id="text">14:25</div>
                    <div data-test-id="text">Бангкок</div>
                    <div data-test-id="text">4&nbsp;мая,&nbsp;пн</div>
                </div>
                """.formatted(ticketAttribute, priceAttribute);
    }

    private AviasalesClientProperties properties() {
        AviasalesClientProperties properties = new AviasalesClientProperties();
        properties.setProvider("aviasales");
        properties.setMaximumTickets(3);
        return properties;
    }

    private ProviderSearchRequest request() {
        return ProviderSearchRequest.builder()
                .airportFrom("SYD")
                .airportTo("BKK")
                .dateFrom(LocalDate.of(2026, 5, 3))
                .dateTo(LocalDate.of(2026, 5, 3))
                .build();
    }
}
