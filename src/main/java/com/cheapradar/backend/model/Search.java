package com.cheapradar.backend.model;

import com.cheapradar.backend.dto.search.SearchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "searches")
@NoArgsConstructor
@AllArgsConstructor
public class Search {
    @Id
    private String id;
    private Long userId;
    private SearchStatus status;

    private String airportFrom;
    private String airportTo;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    private LocalDateTime lastCheckedAt;
    private LocalDateTime nextCheckAt;
    private LocalDateTime checkFinishAt;
    private Integer checkIntervalHours;

    @Convert(converter = StringListConverter.class)
    private List<String> providers;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "search_id")
    private List<Ticket> tickets;

    public Search(String airportFrom, String airportTo,
                  LocalDate dateFrom, LocalDate dateTo, LocalDateTime checkFinishAt,
                  Integer checkIntervalHours, List<String> providers) {

        this.id = UUID.randomUUID().toString();
        this.userId = 1L;
        this.status = SearchStatus.PROCESSING;
        this.airportFrom = airportFrom;
        this.airportTo = airportTo;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.checkFinishAt = checkFinishAt;
        this.checkIntervalHours = checkIntervalHours;
        this.providers = providers;
        this.lastCheckedAt = LocalDateTime.now();

        var nextCheckAt = lastCheckedAt.plusHours(checkIntervalHours);
        if (nextCheckAt.isBefore(checkFinishAt)) {
            this.nextCheckAt = LocalDateTime.now();
        }
    }

    private void setNextCheckAt() {
        var nextCheckAt = lastCheckedAt.plusHours(checkIntervalHours);
        if (nextCheckAt.isBefore(checkFinishAt)) {
            this.nextCheckAt = nextCheckAt;
        } else {
            this.nextCheckAt = null;
        }
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        this.status = SearchStatus.COMPLETED;
        this.lastCheckedAt = nextCheckAt;
        setNextCheckAt();
    }
}
