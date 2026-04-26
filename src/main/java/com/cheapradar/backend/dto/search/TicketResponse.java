package com.cheapradar.backend.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse implements Comparable<TicketResponse>{
    private String airportFrom;
    private String airportTo;
    private LocalDateTime date;
    private BigDecimal price;
    private String provider;
    private String link;

    private String airline;
    private String airlineLogo;

    @Override
    public int compareTo(TicketResponse o) {
        return price.compareTo(o.price);
    }
}
