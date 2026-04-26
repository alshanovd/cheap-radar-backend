package com.cheapradar.backend.mapper;

import com.cheapradar.backend.dto.search.SearchResultsResponse;
import com.cheapradar.backend.dto.search.TicketResponse;
import com.cheapradar.backend.model.Search;
import org.springframework.stereotype.Component;

@Component
public class SearchResultsResponseMapper {
    public SearchResultsResponse map(Search search) {
        return SearchResultsResponse.builder()
                .searchId(search.getId())
                .status(search.getStatus())
                .airportFrom(search.getAirportFrom())
                .airportTo(search.getAirportTo())
                .dateFrom(search.getDateFrom())
                .dateTo(search.getDateTo())
                .checkFinishAt(search.getCheckFinishAt())
                .checkIntervalHours(search.getCheckIntervalHours())
                .createdAt(search.getCreatedAt())
                .lastCheckedAt(search.getLastCheckedAt())
                .nextCheckAt(search.getNextCheckAt())
                .providers(search.getProviders())
                .tickets(
                        search.getTickets().stream().map(ticket -> TicketResponse.builder()
                                .provider(ticket.getProvider())
                                .price(ticket.getPrice())
                                .airportFrom(ticket.getAirportFrom())
                                .airportTo(ticket.getAirportTo())
                                .date(ticket.getDate())
                                .provider(ticket.getProvider())
                                .link(ticket.getLink())
                                .build())
                                .sorted()
                                .toList())
                .build();
    }
}
