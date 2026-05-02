package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.aviasales.model.ZyteExtractRequest;
import com.cheapradar.backend.provider.aviasales.model.ZyteExtractResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class ZyteClient {
    private static final String TRANSFERS_FILTER_SELECTOR = "[data-test-id=\"set-filter-transfers_count\"]";
    private static final String TRANSFERS_FIRST_ROW_SELECTOR = TRANSFERS_FILTER_SELECTOR
            + " [data-test-id=\"set-filter-row-0\"]";
    private static final String TRANSFERS_FIRST_CHECKBOX_SELECTOR =
            TRANSFERS_FIRST_ROW_SELECTOR + " input[type=\"checkbox\"]";
    private static final String RESULTS_LIST_SELECTOR = "[data-test-id=\"search-results-items-list\"]";
    private static final String WAIT_FOR_RESULTS_UPDATE_SCRIPT = """
            new Promise(resolve => {
              const target = document.querySelector('[data-test-id="search-results-items-list"]');
              if (!target) {
                resolve();
                return;
              }
              const timeout = setTimeout(resolve, 5000);
              const observer = new MutationObserver(() => {
                clearTimeout(timeout);
                observer.disconnect();
                resolve();
              });
              observer.observe(target, { childList: true, subtree: true });
            })
            """;

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
                .body(new ZyteExtractRequest(url, true, true, aviasalesActions()))
                .retrieve()
                .body(ZyteExtractResponse.class);

        if (response == null || !isSuccessful(response) || !StringUtils.hasText(response.getBrowserHtml())) {
            throw new IllegalStateException("Zyte extraction failed for url: " + url);
        }

        return response;
    }

    private List<Map<String, Object>> aviasalesActions() {
        return List.of(
                waitForSelectorAction(TRANSFERS_FIRST_ROW_SELECTOR),
                clickAction(TRANSFERS_FIRST_CHECKBOX_SELECTOR),
                evaluateAction(WAIT_FOR_RESULTS_UPDATE_SCRIPT),
                waitForSelectorAction(RESULTS_LIST_SELECTOR)
        );
    }

    private Map<String, Object> waitForSelectorAction(String selector) {
        return Map.of(
                "action", "waitForSelector",
                "selector", cssSelector(selector)
        );
    }

    private Map<String, Object> clickAction(String selector) {
        return Map.of(
                "action", "click",
                "selector", cssSelector(selector)
        );
    }

    private Map<String, Object> evaluateAction(String source) {
        return Map.of(
                "action", "evaluate",
                "source", source
        );
    }

    private Map<String, Object> cssSelector(String value) {
        return Map.of(
                "type", "css",
                "value", value
        );
    }

    private boolean isSuccessful(ZyteExtractResponse response) {
        Integer statusCode = response.getStatusCode();
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }
}
