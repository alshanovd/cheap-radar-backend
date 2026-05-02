package com.cheapradar.backend.model;

import com.cheapradar.backend.dto.search.SearchStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    void replacesTicketsForProviderAndDateAndKeepsOtherDates() {
        LocalDate date = LocalDate.of(2026, 5, 1);
        Search search = new Search();
        search.setStatus(SearchStatus.COMPLETED);
        search.setTickets(List.of(
                ticket("google", "old-google", date.atTime(12, 0)),
                ticket("google", "old-google-next-day", date.plusDays(1).atTime(12, 0)),
                ticket("aviasales", "old-aviasales", date.atTime(12, 0))
        ));

        search.replaceTicketsForProviderAndDate("google", date, List.of(
                ticket("google", "new-google", date.atTime(14, 0))
        ));

        assertEquals(SearchStatus.PROCESSING, search.getStatus());
        assertEquals(List.of("old-google-next-day", "old-aviasales", "new-google"), search.getTickets().stream()
                .map(Ticket::getLink)
                .toList());
    }

    @Test
    void keepsProcessingWhenAnotherCheckIsScheduled() {
        Search search = new Search();
        search.setStatus(SearchStatus.PROCESSING);
        search.setCheckIntervalHours(1);
        search.setCheckFinishAt(LocalDateTime.now().plusHours(2));

        search.completeRun();

        assertEquals(SearchStatus.PROCESSING, search.getStatus());
    }

    @Test
    void completesRunWhenNoNextCheckRemains() {
        Search search = new Search();
        search.setStatus(SearchStatus.PROCESSING);
        search.setCheckIntervalHours(1);
        search.setCheckFinishAt(LocalDateTime.now().plusMinutes(30));

        search.completeRun();

        assertEquals(SearchStatus.COMPLETED, search.getStatus());
    }

    private static Ticket ticket(String provider, String link) {
        return ticket(provider, link, null);
    }

    private static Ticket ticket(String provider, String link, LocalDateTime date) {
        return Ticket.builder()
                .provider(provider)
                .link(link)
                .date(date)
                .price(BigDecimal.TEN)
                .build();
    }
}
