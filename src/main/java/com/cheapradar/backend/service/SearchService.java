package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.search.CreateSearchRequest;
import com.cheapradar.backend.dto.search.GetAllSearchesResponse;
import com.cheapradar.backend.dto.search.SearchResultsResponse;
import com.cheapradar.backend.dto.search.SearchStatus;
import com.cheapradar.backend.mapper.ProviderSearchRequestMapper;
import com.cheapradar.backend.mapper.SearchMapper;
import com.cheapradar.backend.mapper.SearchResultsResponseMapper;
import com.cheapradar.backend.mapper.TicketMapper;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.model.User;
import com.cheapradar.backend.provider.FlightSearchMediator;
import com.cheapradar.backend.provider.dto.MediatorSearchResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import com.cheapradar.backend.repository.SearchRepository;
import com.cheapradar.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchMapper searchMapper;
    private final TicketMapper ticketMapper;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SearchRepository searchRepository;
    private final FlightSearchMediator flightSearchMediator;
    private final ProviderSearchRequestMapper providerSearchRequestMapper;
    private final SearchResultsResponseMapper searchResultsResponseMapper;
    private final Map<String, Object> searchLocks = new ConcurrentHashMap<>();

    public Search createSearch(CreateSearchRequest request) {
        Search search = searchMapper.map(request);
        return searchRepository.save(search);
    }

    public SearchResultsResponse getSearchResults(String searchId) {
            var search = searchRepository.findById(searchId)
                    .orElseThrow(() -> new RuntimeException("Search not found"));
            return searchResultsResponseMapper.map(search);
    }

    public void updateSearchResults(Search search) {
        updateSearchResults(search.getId());
    }

    public void updateSearchResults(String searchId) {
        Search search = withSearchLock(searchId, () -> {
            Search currentSearch = getSearch(searchId);
            if (!currentSearch.isRunnable()) {
                log.info("Skipping search {} with status {}", searchId, currentSearch.getStatus());
                return null;
            }
            currentSearch.markOngoing();
            return searchRepository.save(currentSearch);
        });
        if (search == null) {
            return;
        }

        try {
            ProviderSearchRequest providerSearchRequest = providerSearchRequestMapper.map(search);
            MediatorSearchResult mediatorResult = flightSearchMediator.search(search.getProviders(), providerSearchRequest,
                    (providerSlug, date, tickets) -> saveProviderTickets(searchId, providerSlug, date, tickets));

            if (!mediatorResult.getFailedProviders().isEmpty()) {
                log.warn("Search {} completed with failed provider dates {}",
                        searchId, mediatorResult.getFailedProviderDates());
            }

            if (allProviderDatesFailed(mediatorResult)) {
                withSearchLock(searchId, () -> {
                    Search currentSearch = getSearch(searchId);
                    currentSearch.failRun();
                    return searchRepository.save(currentSearch);
                });
                return;
            }

            Search completedSearch = withSearchLock(searchId, () -> {
                Search currentSearch = getSearch(searchId);
                currentSearch.completeRun();
                return searchRepository.save(currentSearch);
            });

            if (!mediatorResult.getTickets().isEmpty()) {
                sendSearchResultEmail(completedSearch);
            }
        } catch (RuntimeException exception) {
            log.error("Search {} failed while updating results", searchId, exception);
            withSearchLock(searchId, () -> {
                Search currentSearch = getSearch(searchId);
                if (currentSearch.getStatus() == SearchStatus.ONGOING) {
                    currentSearch.failRun();
                    return searchRepository.save(currentSearch);
                }
                return currentSearch;
            });
        }
    }

    private boolean allProviderDatesFailed(MediatorSearchResult mediatorResult) {
        return mediatorResult.getTickets().isEmpty()
                && !mediatorResult.getFailedProviderDates().isEmpty()
                && mediatorResult.getSuccessfulProviderDates().isEmpty();
    }

    public GetAllSearchesResponse getAllSearches(Long userId) {
        List<Search> searches = searchRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return GetAllSearchesResponse.builder()
                        .searches(searches.stream()
                                .map(searchResultsResponseMapper::map)
                                .toList())
                .build();
    }

    @Scheduled(fixedRate = 600000)
    public void scheduledUpdateSearchResults() {
        List<Search> searches = searchRepository.findAllByStatusInAndNextCheckAtBefore(
                List.of(SearchStatus.CREATED, SearchStatus.SCHEDULED),
                LocalDateTime.now());
        searches.forEach(search ->
                CompletableFuture.runAsync(() -> updateSearchResults(search))
        );
    }

    private void saveProviderTickets(String searchId, String providerSlug, LocalDate date,
                                     List<ProviderTicket> providerTickets) {
        withSearchLock(searchId, () -> {
            Search search = getSearch(searchId);
            List<Ticket> tickets = providerTickets.stream()
                    .filter(ticket -> ticket.getDate() != null && date.equals(ticket.getDate().toLocalDate()))
                    .map(ticketMapper::map)
                    .peek(ticket -> ticket.setProvider(providerSlug))
                    .toList();
            search.replaceTicketsForProviderAndDate(providerSlug, date, tickets);
            return searchRepository.save(search);
        });
    }

    private void sendSearchResultEmail(Search search) {
        List<Ticket> tickets = search.getTickets() == null ? List.of() : search.getTickets();
        Long userId = search.getUserId();
        if (userId != null && !tickets.isEmpty()) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                emailService.sendSearchResultEmail(user.get(), search);
            } else {
                log.warn("User with id {} not found, cannot send email", userId);
            }
        }
    }

    private Search getSearch(String searchId) {
        return searchRepository.findById(searchId)
                .orElseThrow(() -> new RuntimeException("Search not found"));
    }

    private <T> T withSearchLock(String searchId, Supplier<T> action) {
        synchronized (searchLocks.computeIfAbsent(searchId, id -> new Object())) {
            return action.get();
        }
    }
}
