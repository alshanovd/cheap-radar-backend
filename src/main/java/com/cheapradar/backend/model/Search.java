package com.cheapradar.backend.model;

import com.cheapradar.backend.dto.search.SearchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@ToString
@Table(name = "searches")
@NoArgsConstructor
@AllArgsConstructor
public class Search {
    @Id
    private String id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private SearchStatus status;

    private String airportFrom;
    private String airportTo;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    private LocalDateTime createdAt;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime nextCheckAt;
    private Integer checkIntervalHours;
    private Integer checkCount;
    private Integer completedCheckCount;

    @Convert(converter = StringListConverter.class)
    private List<String> providers;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "search_id")
    private List<Ticket> tickets;

    public Search(String airportFrom, String airportTo,
                  LocalDate dateFrom, LocalDate dateTo,
                  Integer checkIntervalHours, Integer checkCount, List<String> providers) {

        this.id = UUID.randomUUID().toString();
        this.userId = 1L;
        this.status = SearchStatus.CREATED;
        this.airportFrom = airportFrom;
        this.airportTo = airportTo;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.checkIntervalHours = checkIntervalHours;
        this.checkCount = checkCount;
        this.completedCheckCount = 0;
        this.providers = providers;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastCheckedAt = null;
        this.nextCheckAt = now;
    }

    private void setNextCheckAt() {
        if (completedCheckCount != null && checkCount != null && completedCheckCount < checkCount) {
            this.nextCheckAt = lastCheckedAt.plusHours(checkIntervalHours);
        } else {
            this.nextCheckAt = null;
        }
    }

    private void incrementCompletedCheckCount() {
        this.completedCheckCount = completedCheckCount == null ? 1 : completedCheckCount + 1;
    }

    public void setTickets(List<Ticket> tickets, SearchStatus status) {
        this.tickets = tickets;
        this.status = status;
        this.lastCheckedAt = LocalDateTime.now();
        incrementCompletedCheckCount();
        setNextCheckAt();
    }

    public void markOngoing() {
        this.status = SearchStatus.ONGOING;
    }

    public boolean isRunnable() {
        return status == SearchStatus.CREATED || status == SearchStatus.SCHEDULED;
    }

    public void failRun() {
        this.lastCheckedAt = LocalDateTime.now();
        incrementCompletedCheckCount();
        setNextCheckAt();
        this.status = nextCheckAt == null ? SearchStatus.FAILED : SearchStatus.SCHEDULED;
    }

    public void replaceTicketsForProvider(String provider, List<Ticket> providerTickets) {
        List<Ticket> updatedTickets = tickets == null ? new ArrayList<>() : new ArrayList<>(tickets);
        updatedTickets.removeIf(ticket -> Objects.equals(ticket.getProvider(), provider));
        updatedTickets.addAll(providerTickets);
        this.tickets = updatedTickets;
        this.status = SearchStatus.ONGOING;
    }

    public void replaceTicketsForProviderAndDate(String provider, LocalDate date, List<Ticket> providerTickets) {
        List<Ticket> updatedTickets = tickets == null ? new ArrayList<>() : new ArrayList<>(tickets);
        updatedTickets.removeIf(ticket -> Objects.equals(ticket.getProvider(), provider)
                && ticket.getDate() != null
                && date.equals(ticket.getDate().toLocalDate()));
        updatedTickets.addAll(providerTickets);
        this.tickets = updatedTickets;
        this.status = SearchStatus.ONGOING;
    }

    public void completeRun() {
        this.lastCheckedAt = LocalDateTime.now();
        incrementCompletedCheckCount();
        setNextCheckAt();
        this.status = nextCheckAt == null ? SearchStatus.COMPLETED : SearchStatus.SCHEDULED;
    }

    public LocalDateTime getCheckFinishAt() {
        if (createdAt == null || checkCount == null || checkIntervalHours == null) {
            return null;
        }
        return createdAt.plusHours((long) (checkCount - 1) * checkIntervalHours);
    }
}
