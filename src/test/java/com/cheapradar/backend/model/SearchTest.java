package com.cheapradar.backend.model;

import com.cheapradar.backend.dto.search.SearchStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchTest {
    @Test
    void replacesTicketsForProviderAndKeepsOtherProviders() {
        Search search = new Search();
        search.setStatus(SearchStatus.COMPLETED);
        search.setTickets(List.of(
                ticket("google", "old-google"),
                ticket("aviasales", "old-aviasales")
        ));

        search.replaceTicketsForProvider("google", List.of(ticket("google", "new-google")));

        assertEquals(SearchStatus.PROCESSING, search.getStatus());
        assertEquals(List.of("old-aviasales", "new-google"), search.getTickets().stream()
                .map(Ticket::getLink)
                .toList());
    }

    @Test
    void completesRunAndAdvancesStatus() {
        Search search = new Search();
        search.setStatus(SearchStatus.PROCESSING);
        search.setCheckIntervalHours(1);
        search.setCheckFinishAt(LocalDateTime.now().plusHours(2));

        search.completeRun();

        assertEquals(SearchStatus.COMPLETED, search.getStatus());
    }

    private static Ticket ticket(String provider, String link) {
        return Ticket.builder()
                .provider(provider)
                .link(link)
                .price(BigDecimal.TEN)
                .build();
    }
}
