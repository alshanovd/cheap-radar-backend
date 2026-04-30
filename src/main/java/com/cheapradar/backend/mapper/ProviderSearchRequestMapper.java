package com.cheapradar.backend.mapper;

import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class ProviderSearchRequestMapper {
    public ProviderSearchRequest map(Search search) {
        return ProviderSearchRequest.builder()
                .airportFrom(search.getAirportFrom())
                .airportTo(search.getAirportTo())
                .dateFrom(search.getDateFrom())
                .dateTo(search.getDateTo())
                .build();
    }
}
