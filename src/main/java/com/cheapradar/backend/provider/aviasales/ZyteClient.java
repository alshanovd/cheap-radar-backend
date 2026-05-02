package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.aviasales.model.ZyteExtractRequest;
import com.cheapradar.backend.provider.aviasales.model.ZyteExtractResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class ZyteClient {
    private final RestClient restClient;

    @Autowired
    public ZyteClient(AviasalesClientProperties properties) {
        this(createRestClient(properties));
    }

    ZyteClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient createRestClient(AviasalesClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getApiKey(), ""))
                .build();
    }

    public ZyteExtractResponse extract(String url) {
        ZyteExtractResponse response = restClient.post()
                .uri("")
                .body(new ZyteExtractRequest(url, true, true))
                .retrieve()
                .body(ZyteExtractResponse.class);

        if (response == null || !isSuccessful(response) || !StringUtils.hasText(response.getBrowserHtml())) {
            throw new IllegalStateException("Zyte extraction failed for url: " + url);
        }

        return response;
    }

    private boolean isSuccessful(ZyteExtractResponse response) {
        Integer statusCode = response.getStatusCode();
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }
}
