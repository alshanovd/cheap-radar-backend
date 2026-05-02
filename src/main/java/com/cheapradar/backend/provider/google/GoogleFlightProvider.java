package com.cheapradar.backend.provider.google;

import com.cheapradar.backend.provider.FlightProvider;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.google.model.GoogleSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class GoogleFlightProvider implements FlightProvider {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final RestClient restClient;
    private final GoogleClientProperties properties;
    private final GoogleSearchResponseMapper googleSearchResponseMapper;

    public GoogleFlightProvider(GoogleClientProperties properties, GoogleSearchResponseMapper googleSearchResponseMapper) {
        this.googleSearchResponseMapper = googleSearchResponseMapper;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .build();
    }

    @Override
    public String slug() {
        return properties.getProvider().toLowerCase(Locale.ROOT);
    }

    @Override
    public ProviderSearchResponse search(ProviderSearchRequest request) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        MultiValueMap<String, String> queryParams = getQueryParams(request);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = request.getDateFrom();
        LocalDate end = request.getDateTo();

        int limit = properties.getMaximumRequests();
        while (!start.isAfter(end) && limit > 0) {
            dates.add(start);
            start = start.plusDays(1);
            limit--;
        }

        List<CompletableFuture<GoogleSearchResponse>> futures = dates.stream()
                .map(date -> {
                    MultiValueMap<String, String> paramsWithDate = new LinkedMultiValueMap<>(queryParams);
                    paramsWithDate.set("outbound_date", date.format(dateFormatter));
                    return CompletableFuture.supplyAsync(() ->
                            restClient.get()
                                    .uri(uriBuilder -> uriBuilder
                                            .queryParams(paramsWithDate)
                                            .build())
                                    .retrieve()
                                    .body(GoogleSearchResponse.class)
                    );
                })
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<GoogleSearchResponse> allResponses = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        return googleSearchResponseMapper.map(allResponses);
    }

    private MultiValueMap<String, String> getQueryParams(ProviderSearchRequest request) {
        LinkedMultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();
        multiMap.add("departure_id", request.getAirportFrom());
        multiMap.add("arrival_id", request.getAirportTo());
        multiMap.add("key", properties.getApiKey());
        multiMap.add("engine", "google_flights");
        multiMap.add("currency", "USD");
        multiMap.add("sort_by", "2");
        multiMap.add("adults", "1");
        multiMap.add("stops", "1");
        multiMap.add("type", "2");
        multiMap.add("gl", "au");
        multiMap.add("hl", "en");
        return multiMap;
    }
}
