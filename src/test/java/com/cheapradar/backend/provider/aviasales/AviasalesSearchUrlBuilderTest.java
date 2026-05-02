package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AviasalesSearchUrlBuilderTest {
    @Test
    void buildsAviasalesSearchUrl() {
        AviasalesClientProperties properties = new AviasalesClientProperties();
        properties.setSearchBaseUrl("https://www.aviasales.ru/search");
        AviasalesSearchUrlBuilder builder = new AviasalesSearchUrlBuilder(properties);
        ProviderSearchRequest request = ProviderSearchRequest.builder()
                .airportFrom("SYD")
                .airportTo("BKK")
                .build();

        String url = builder.build(request, LocalDate.of(2026, 5, 3));

        assertEquals("https://www.aviasales.ru/search/SYD0305BKK1?currency=usd&destination_airports=0", url);
    }
}
