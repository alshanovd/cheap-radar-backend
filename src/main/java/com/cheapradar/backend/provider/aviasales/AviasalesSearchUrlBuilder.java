package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AviasalesSearchUrlBuilder {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMM");

    private final AviasalesClientProperties properties;

    public String build(ProviderSearchRequest request, LocalDate date) {
        return "%s/%s%s%s1?currency=usd&destination_airports=0".formatted(
                properties.getSearchBaseUrl(),
                request.getAirportFrom().toUpperCase(Locale.ROOT),
                date.format(DATE_FORMAT),
                request.getAirportTo().toUpperCase(Locale.ROOT)
        );
    }
}
