package com.cheapradar.backend.service;

import com.cheapradar.backend.client.SearchClient;
import com.cheapradar.backend.client.dto.ClientSearchRequest;
import com.cheapradar.backend.client.dto.ClientSearchResponse;
import com.cheapradar.backend.dto.search.*;
import com.cheapradar.backend.mapper.ClientSearchRequestMapper;
import com.cheapradar.backend.mapper.SearchMapper;
import com.cheapradar.backend.mapper.SearchResultsResponseMapper;
import com.cheapradar.backend.mapper.TicketMapper;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.model.User;
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
import java.util.Set;
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
    private final Map<String, SearchClient> searchClients;
    private final ClientSearchRequestMapper clientSearchRequestMapper;
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
        ClientSearchRequest clientSearchRequest = clientSearchRequestMapper.map(search);
        List<String> providers = search.getProviders();
        Set<SearchClient> clients = searchClients.entrySet().stream()
                .filter(entry -> providers.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        List<CompletableFuture<ClientSearchResponse>> futures = clients.stream()
                .map(client -> CompletableFuture.supplyAsync(() -> client.search(clientSearchRequest)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<Ticket> allTickets = futures.stream()
                .map(CompletableFuture::join)
                .filter(response -> response != null && response.getTickets() != null)
                .flatMap(response -> response.getTickets().stream())
                .map(ticketMapper::map)
                .collect(Collectors.toList());

        search.setTickets(allTickets);
        searchRepository.save(search);

        Long userId = search.getUserId();
        if (userId != null) {
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
}
