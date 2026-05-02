package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.FlightProvider;
import com.cheapradar.backend.provider.aviasales.AviasalesSearchResponseMapper.AviasalesHtmlResponse;
import com.cheapradar.backend.provider.aviasales.model.ZyteExtractResponse;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

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
        String searchUrl = searchUrlBuilder.build(request, request.getDateFrom());
        ZyteExtractResponse response = zyteClient.extract(searchUrl);
        AviasalesHtmlResponse htmlResponse = new AviasalesHtmlResponse(
                response.getBrowserHtml(), searchUrl, request.getDateFrom());

        return mapper.map(List.of(htmlResponse), request);
    }
}
