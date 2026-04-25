package com.cheapradar.backend.client.test;

import com.cheapradar.backend.client.SearchClient;
import com.cheapradar.backend.client.dto.ClientSearchRequest;
import com.cheapradar.backend.client.dto.ClientSearchResponse;
import com.cheapradar.backend.dto.search.TicketResponse;

import java.math.BigDecimal;
import java.util.List;

public class TestSearchClient implements SearchClient {

    private static final String PROVIDER = "TEST";

    @Override
    public ClientSearchResponse search(ClientSearchRequest request) {
        return ClientSearchResponse.builder()
                .tickets(List.of(
                        TicketResponse.builder()
                                .provider(PROVIDER)
                                .airportFrom(request.getAirportFrom())
                                .airportTo(request.getAirportTo())
                                .date(request.getDateFrom().atStartOfDay())
                                .link("https://www.example.com/ticket")
                                .price(BigDecimal.valueOf(100))
                                .build()))
                .build();
    }
}
