package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.search.SearchStatus;
import com.cheapradar.backend.mapper.ProviderSearchRequestMapper;
import com.cheapradar.backend.mapper.SearchMapper;
import com.cheapradar.backend.mapper.SearchResultsResponseMapper;
import com.cheapradar.backend.mapper.TicketMapper;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.model.User;
import com.cheapradar.backend.provider.FlightSearchMediator;
import com.cheapradar.backend.provider.MediatorResultHandler;
import com.cheapradar.backend.provider.dto.MediatorSearchResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import com.cheapradar.backend.repository.SearchRepository;
import com.cheapradar.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_2 = LocalDate.of(2026, 5, 2);

    private final SearchMapper searchMapper = mock(SearchMapper.class);
    private final TicketMapper ticketMapper = new TicketMapper();
    private final EmailService emailService = mock(EmailService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SearchRepository searchRepository = mock(SearchRepository.class);
    private final FlightSearchMediator flightSearchMediator = mock(FlightSearchMediator.class);
    private final ProviderSearchRequestMapper providerSearchRequestMapper = mock(ProviderSearchRequestMapper.class);
    private final SearchResultsResponseMapper searchResultsResponseMapper = mock(SearchResultsResponseMapper.class);

    private final SearchService service = new SearchService(
            searchMapper,
            ticketMapper,
            emailService,
            userRepository,
            searchRepository,
            flightSearchMediator,
            providerSearchRequestMapper,
            searchResultsResponseMapper
    );

    @Test
    void persistsSuccessfulProviderDateResultsAndKeepsProcessingWhenMoreChecksRemain() {
        Search search = search();
        ProviderSearchRequest request = ProviderSearchRequest.builder().build();
        ProviderTicket googleTicket = providerTicket("google", "new-google", MAY_1.atTime(12, 0));
        ProviderTicket mismatchedTicket = providerTicket("google", "wrong-date-google", MAY_2.atTime(12, 0));

        when(searchRepository.findById("search-id")).thenReturn(Optional.of(search));
        when(searchRepository.save(any(Search.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerSearchRequestMapper.map(search)).thenReturn(request);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Danil", "danil@example.com")));
        when(flightSearchMediator.search(eq(List.of("google", "test")), eq(request), any(MediatorResultHandler.class)))
                .thenAnswer(invocation -> {
                    MediatorResultHandler handler = invocation.getArgument(2);
                    handler.onSuccess("google", MAY_1, List.of(googleTicket, mismatchedTicket));
                    assertEquals(SearchStatus.PROCESSING, search.getStatus());
                    return MediatorSearchResult.builder()
                            .tickets(List.of(googleTicket))
                            .successfulProviders(new LinkedHashSet<>(List.of("google")))
                            .failedProviders(new LinkedHashSet<>(List.of("test")))
                            .failedProviderDates(new LinkedHashSet<>(List.of(
                                    new MediatorSearchResult.ProviderDateResult("test", MAY_1)
                            )))
                            .build();
                });

        service.updateSearchResults(search);

        assertEquals(SearchStatus.SCHEDULED, search.getStatus());
        assertEquals(List.of("old-google-next-day", "old-aviasales", "new-google"), search.getTickets().stream()
                .map(Ticket::getLink)
                .toList());
        verify(searchRepository, atLeast(3)).save(search);
        verify(emailService).sendSearchResultEmail(any(User.class), eq(search));
    }

    private static Search search() {
        Search search = new Search();
        search.setId("search-id");
        search.setUserId(1L);
        search.setStatus(SearchStatus.COMPLETED);
        search.setProviders(List.of("google", "test"));
        search.setTickets(List.of(
                ticket("google", "old-google", MAY_1.atTime(10, 0)),
                ticket("google", "old-google-next-day", MAY_2.atTime(10, 0)),
                ticket("aviasales", "old-aviasales", MAY_1.atTime(10, 0))
        ));
        search.setCheckIntervalHours(1);
        search.setCheckFinishAt(LocalDateTime.now().plusHours(2));
        return search;
    }

    private static Ticket ticket(String provider, String link, LocalDateTime date) {
        return Ticket.builder()
                .provider(provider)
                .link(link)
                .date(date)
                .price(BigDecimal.TEN)
                .build();
    }

    private static ProviderTicket providerTicket(String provider, String link, LocalDateTime date) {
        return ProviderTicket.builder()
                .provider(provider)
                .link(link)
                .date(date)
                .price(BigDecimal.ONE)
                .build();
    }
}
