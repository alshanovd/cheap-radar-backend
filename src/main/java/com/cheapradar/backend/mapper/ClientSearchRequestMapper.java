package com.cheapradar.backend.mapper;

import com.cheapradar.backend.client.dto.ClientSearchRequest;
import com.cheapradar.backend.model.Search;
import org.springframework.stereotype.Component;

@Component
public class ClientSearchRequestMapper {
    public ClientSearchRequest map(Search search) {
        return ClientSearchRequest.builder()
                .airportFrom(search.getAirportFrom())
                .airportTo(search.getAirportTo())
                .dateFrom(search.getDateFrom())
                .dateTo(search.getDateTo())
                .build();
    }
}
