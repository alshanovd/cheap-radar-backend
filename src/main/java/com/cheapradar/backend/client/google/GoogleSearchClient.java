package com.cheapradar.backend.client.google;

import com.cheapradar.backend.client.SearchClient;
import com.cheapradar.backend.client.dto.ClientSearchRequest;
import com.cheapradar.backend.client.dto.ClientSearchResponse;
import com.cheapradar.backend.client.google.mapping.ClientSearchResponseMapper;
import com.cheapradar.backend.client.google.model.GoogleSearchResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GoogleSearchClient implements SearchClient {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final RestClient restClient;
    private final GoogleClientProperties properties;
    private final ClientSearchResponseMapper clientSearchResponseMapper;

    public GoogleSearchClient(GoogleClientProperties properties, ClientSearchResponseMapper clientSearchResponseMapper) {
        this.clientSearchResponseMapper = clientSearchResponseMapper;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .build();
    }

    @Override
    public ClientSearchResponse search(ClientSearchRequest request) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        MultiValueMap<String, String> queryParams = getQueryParams(request);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = request.getDateFrom();
        LocalDate end = request.getDateTo();
        int limit = 2;
        while (!start.isAfter(end) && limit > 0) {
            dates.add(start);
            start = start.plusDays(1);
            limit --;
        }

        List<CompletableFuture<String>> futures = dates.stream()
                .map(date -> {
                    MultiValueMap<String, String> paramsWithDate = new LinkedMultiValueMap<>(queryParams);
                    paramsWithDate.set("outbound_date", date.format(dateFormatter));
                    return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .queryParams(paramsWithDate)
                                    .build())
                            .retrieve()
                            .body(String.class)
                    );
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<GoogleSearchResponse> allResponses = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .peek(System.out::println)
                .map(response -> new ObjectMapper().readValue(response, GoogleSearchResponse.class))
                .toList();

        return clientSearchResponseMapper.map(allResponses);
    }

    private MultiValueMap<String, String> getQueryParams(ClientSearchRequest request) {
        LinkedMultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();
        multiMap.add("departure_id", request.getAirportFrom());
        multiMap.add("arrival_id", request.getAirportTo());
        multiMap.add("key", properties.getApiKey());
        multiMap.add("engine", "google_flights");
        multiMap.add("currency", "AUD");
        multiMap.add("sort_by", "2");
        multiMap.add("adults", "1");
        multiMap.add("stops", "1");
        multiMap.add("type", "2");
        multiMap.add("gl", "au");
        multiMap.add("hl", "en");
        return multiMap;
    }
}
