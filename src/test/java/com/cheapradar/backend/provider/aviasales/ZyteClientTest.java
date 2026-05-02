package com.cheapradar.backend.provider.aviasales;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZyteClientTest {
    private static final String ENDPOINT = "https://api.zyte.com/v1/extract";
    private static final String SEARCH_URL = "https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0";

    @Test
    void sendsZyteExtractionRequest() {
        RestClient.Builder builder = restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ZyteClient client = new ZyteClient(builder.build());
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth("test-key")))
                .andExpect(content().json("""
                        {
                          "url": "https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0",
                          "browserHtml": true,
                          "javascript": true
                        }
                        """))
                .andExpect(jsonPath("$.actions[0].action").value("waitForSelector"))
                .andExpect(jsonPath("$.actions[0].selector.value")
                        .value("[data-test-id=\"set-filter-transfers_count\"]"))
                .andExpect(jsonPath("$.actions[1].action").value("click"))
                .andExpect(jsonPath("$.actions[1].selector.value")
                        .value("[data-test-id=\"set-filter-transfers_count\"] [data-test-id=\"set-filter-row-0\"] "
                                + "input[type=\"checkbox\"]"))
                .andExpect(jsonPath("$.actions[2].action").value("evaluate"))
                .andExpect(jsonPath("$.actions[2].source").isNotEmpty())
                .andExpect(jsonPath("$.actions[3].action").value("waitForSelector"))
                .andExpect(jsonPath("$.actions[3].selector.value").value("[data-test-id=\"search-results-items-list\"]"))
                .andRespond(withSuccess("""
                        {
                          "url": "https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0",
                          "statusCode": 200,
                          "browserHtml": "<html></html>"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertEquals("<html></html>", client.extract(SEARCH_URL).getBrowserHtml());
        server.verify();
    }

    @Test
    void throwsWhenZyteTargetStatusIsNotSuccessful() {
        RestClient.Builder builder = restClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ZyteClient client = new ZyteClient(builder.build());
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("""
                        {
                          "url": "https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0",
                          "statusCode": 500,
                          "browserHtml": "<html></html>"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> client.extract(SEARCH_URL));
        server.verify();
    }

    private RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .baseUrl(ENDPOINT)
                .defaultHeaders(headers -> headers.setBasicAuth("test-key", ""));
    }

    private String basicAuth(String username) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":").getBytes(StandardCharsets.UTF_8));
    }
}
