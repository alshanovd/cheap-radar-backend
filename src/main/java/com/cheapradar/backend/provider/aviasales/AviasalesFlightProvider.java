package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.FlightProvider;
import com.cheapradar.backend.provider.aviasales.AviasalesSearchResponseMapper.AviasalesHtmlResponse;
import com.cheapradar.backend.provider.aviasales.model.ZyteExtractResponse;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Component
public class AviasalesFlightProvider implements FlightProvider {
    private final AviasalesClientProperties properties;
    private final AviasalesSearchUrlBuilder searchUrlBuilder;
    private final ZyteClient zyteClient;
    private final AviasalesSearchResponseMapper mapper;

    public AviasalesFlightProvider(
            AviasalesClientProperties properties,
            AviasalesSearchUrlBuilder searchUrlBuilder,
            ZyteClient zyteClient,
            AviasalesSearchResponseMapper mapper
    ) {
        this.properties = properties;
        this.searchUrlBuilder = searchUrlBuilder;
        this.zyteClient = zyteClient;
        this.mapper = mapper;
    }

    @Override
    public String slug() {
        return properties.getProvider().toLowerCase(Locale.ROOT);
    }

    @Override
    public ProviderSearchResponse search(ProviderSearchRequest request) {
        List<LocalDate> dates = getDates(request);

        List<CompletableFuture<AviasalesHtmlResponse>> futures = dates.stream()
                .map(date -> CompletableFuture.supplyAsync(() -> searchDate(request, date)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<AviasalesHtmlResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return mapper.map(responses, request);
    }

    private AviasalesHtmlResponse searchDate(ProviderSearchRequest request, LocalDate date) {
        String searchUrl = searchUrlBuilder.build(request, date);
        ZyteExtractResponse response = zyteClient.extract(searchUrl);
        return new AviasalesHtmlResponse(response.getBrowserHtml(), searchUrl, date);
    }

    private List<LocalDate> getDates(ProviderSearchRequest request) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = request.getDateFrom();
        LocalDate end = request.getDateTo();

        int limit = properties.getMaximumRequests();
        while (!start.isAfter(end) && limit > 0) {
            dates.add(start);
            start = start.plusDays(1);
            limit--;
        }

        return dates;
    }
}
