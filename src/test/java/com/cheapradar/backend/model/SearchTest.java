package com.cheapradar.backend.model;

import com.cheapradar.backend.dto.search.SearchStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SearchTest {
    @Test
    void startsCreatedWithNoLastCheckedAtAndImmediateNextCheck() {
        Search search = new Search(
                "SYD",
                "MEL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                1,
                3,
                List.of("google")
        );

        assertEquals(SearchStatus.CREATED, search.getStatus());
        assertNull(search.getLastCheckedAt());
        assertEquals(search.getCreatedAt(), search.getNextCheckAt());
        assertEquals(0, search.getCompletedCheckCount());
    }

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
    void schedulesRunWhenAnotherCheckIsScheduled() {
        Search search = new Search();
        search.setStatus(SearchStatus.PROCESSING);
        search.setCheckIntervalHours(1);
        search.setCheckCount(2);
        search.setCompletedCheckCount(0);

        search.completeRun();

        assertEquals(SearchStatus.SCHEDULED, search.getStatus());
        assertEquals(1, search.getCompletedCheckCount());
        assertEquals(search.getLastCheckedAt().plusHours(1), search.getNextCheckAt());
    }

    @Test
    void completesRunWhenNoNextCheckRemains() {
        Search search = new Search();
        search.setStatus(SearchStatus.PROCESSING);
        search.setCheckIntervalHours(1);
        search.setCheckCount(1);
        search.setCompletedCheckCount(0);

        search.completeRun();

        assertEquals(SearchStatus.COMPLETED, search.getStatus());
        assertEquals(1, search.getCompletedCheckCount());
        assertNull(search.getNextCheckAt());
    }

    @Test
    void calculatesCheckFinishAtFromCreatedAtCountAndInterval() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 30, 9, 0);
        Search search = new Search();
        search.setCreatedAt(createdAt);
        search.setCheckIntervalHours(3);
        search.setCheckCount(4);

        assertEquals(LocalDateTime.of(2026, 4, 30, 18, 0), search.getCheckFinishAt());
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
