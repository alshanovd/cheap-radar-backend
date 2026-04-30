package com.cheapradar.backend.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProviderTicket implements Comparable<ProviderTicket> {
    private String airportFrom;
    private String airportTo;
    private LocalDateTime date;
    private BigDecimal price;
    private String provider;
    private String link;
    private String airline;
    private String airlineLogo;

    @Override
    public int compareTo(ProviderTicket other) {
        return price.compareTo(other.price);
    }
}
