package com.cheapradar.backend.mapper;

import com.cheapradar.backend.dto.search.SearchResultsResponse;
import com.cheapradar.backend.dto.search.TicketResponse;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchResultsResponseMapper {
    public SearchResultsResponse map(Search search) {
        List<Ticket> tickets = search.getTickets() == null ? List.of() : search.getTickets();

        return SearchResultsResponse.builder()
                .searchId(search.getId())
                .status(search.getStatus())
                .airportFrom(search.getAirportFrom())
                .airportTo(search.getAirportTo())
                .dateFrom(search.getDateFrom())
                .dateTo(search.getDateTo())
                .checkFinishAt(search.getCheckFinishAt())
                .checkIntervalHours(search.getCheckIntervalHours())
                .checkCount(search.getCheckCount())
                .completedCheckCount(search.getCompletedCheckCount())
                .createdAt(search.getCreatedAt())
                .lastCheckedAt(search.getLastCheckedAt())
                .nextCheckAt(search.getNextCheckAt())
                .providers(search.getProviders())
                .tickets(
                        tickets.stream().map(ticket -> TicketResponse.builder()
                                        .provider(ticket.getProvider())
                                        .price(ticket.getPrice())
                                        .airline(ticket.getAirline())
                                        .airlineLogo(ticket.getAirlineLogo())
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
