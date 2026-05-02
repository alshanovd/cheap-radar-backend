package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.search.CreateSearchRequest;
import com.cheapradar.backend.dto.search.GetAllSearchesResponse;
import com.cheapradar.backend.dto.search.SearchResultsResponse;
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
            currentSearch.markProcessing();
            return searchRepository.save(currentSearch);
        });

        ProviderSearchRequest providerSearchRequest = providerSearchRequestMapper.map(search);
        MediatorSearchResult mediatorResult = flightSearchMediator.search(search.getProviders(), providerSearchRequest,
                (providerSlug, tickets) -> saveProviderTickets(searchId, providerSlug, tickets));

        if (!mediatorResult.getFailedProviders().isEmpty()) {
            log.warn("Search {} completed with failed providers {}", searchId, mediatorResult.getFailedProviders());
        }

        Search completedSearch = withSearchLock(searchId, () -> {
            Search currentSearch = getSearch(searchId);
            currentSearch.completeRun();
            return searchRepository.save(currentSearch);
        });

        if (!mediatorResult.getTickets().isEmpty()) {
            sendSearchResultEmail(completedSearch);
        }
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
        List<Search> searches = searchRepository.findAllByNextCheckAtBefore(LocalDateTime.now());
        searches.forEach(search ->
                CompletableFuture.runAsync(() -> updateSearchResults(search))
        );
    }

    private void saveProviderTickets(String searchId, String providerSlug, List<ProviderTicket> providerTickets) {
        withSearchLock(searchId, () -> {
            Search search = getSearch(searchId);
            List<Ticket> tickets = providerTickets.stream()
                    .map(ticketMapper::map)
                    .peek(ticket -> ticket.setProvider(providerSlug))
                    .toList();
            search.replaceTicketsForProvider(providerSlug, tickets);
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
