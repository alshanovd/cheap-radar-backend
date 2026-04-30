package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.search.*;
import com.cheapradar.backend.mapper.ProviderSearchRequestMapper;
import com.cheapradar.backend.mapper.SearchMapper;
import com.cheapradar.backend.mapper.SearchResultsResponseMapper;
import com.cheapradar.backend.mapper.TicketMapper;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.model.User;
import com.cheapradar.backend.provider.ProviderProxy;
import com.cheapradar.backend.provider.dto.ProviderAggregateResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.repository.SearchRepository;
import com.cheapradar.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchMapper searchMapper;
    private final TicketMapper ticketMapper;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SearchRepository searchRepository;
    private final ProviderProxy providerProxy;
    private final ProviderSearchRequestMapper providerSearchRequestMapper;
    private final SearchResultsResponseMapper searchResultsResponseMapper;

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
        ProviderSearchRequest providerSearchRequest = providerSearchRequestMapper.map(search);
        ProviderAggregateResult providerResult = providerProxy.search(search.getProviders(), providerSearchRequest);

        List<Ticket> allTickets = providerResult.getTickets().stream()
                .map(ticketMapper::map)
                .collect(Collectors.toList());

        search.setTickets(allTickets, getSearchStatus(providerResult));
        searchRepository.save(search);

        Long userId = search.getUserId();
        if (userId != null && !allTickets.isEmpty()) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                emailService.sendSearchResultEmail(user.get(), search);
            } else {
                log.warn("User with id {} not found, cannot send email", userId);
            }
        }
    }

    public GetAllSearchesResponse getAllSearches(Long userId) {
        List<Search> searches = searchRepository.findAllByUserId(userId);
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

    private SearchStatus getSearchStatus(ProviderAggregateResult providerResult) {
        if (providerResult.getSuccessfulProviders().isEmpty()) {
            return SearchStatus.FAILED;
        }
        if (!providerResult.getFailedProviders().isEmpty()) {
            return SearchStatus.PARTIAL;
        }
        return SearchStatus.COMPLETED;
    }
}
