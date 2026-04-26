package com.cheapradar.backend.dto.search;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SearchResultsResponse {
    private String searchId;
    private SearchStatus status;
    private String airportFrom;
    private String airportTo;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    private LocalDateTime createdAt;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime nextCheckAt;
    private LocalDateTime checkFinishAt;
    private Integer checkIntervalHours;
    private List<String> providers;

    private List<TicketResponse> tickets;

}
