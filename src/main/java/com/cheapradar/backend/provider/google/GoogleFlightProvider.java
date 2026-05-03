package com.cheapradar.backend.provider.google;

import com.cheapradar.backend.provider.FlightProvider;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.google.model.GoogleSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class GoogleFlightProvider implements FlightProvider {
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String PRICE_SORT = "2";

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
        queryParams.set("outbound_date", request.getDateFrom().format(dateFormatter));

        GoogleSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParams(queryParams)
                        .build())
                .retrieve()
                .body(GoogleSearchResponse.class);

        return googleSearchResponseMapper.map(response == null ? List.of() : List.of(response));
    }

    private MultiValueMap<String, String> getQueryParams(ProviderSearchRequest request) {
        LinkedMultiValueMap<String, String> multiMap = new LinkedMultiValueMap<>();
        multiMap.add("departure_id", request.getAirportFrom());
        multiMap.add("arrival_id", request.getAirportTo());
        multiMap.add("key", properties.getApiKey());
        multiMap.add("engine", "google_flights");
        multiMap.add("currency", "USD");
        multiMap.add("sort_by", PRICE_SORT);
        multiMap.add("adults", "1");
        multiMap.add("stops", "0");
        multiMap.add("type", "2");
        multiMap.add("gl", "us");
        multiMap.add("hl", "en");
        return multiMap;
    }
}
