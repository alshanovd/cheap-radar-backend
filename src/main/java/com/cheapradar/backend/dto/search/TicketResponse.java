package com.cheapradar.backend.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private String airportFrom;
    private String airportTo;
    private LocalDateTime date;
    private String provider;
    private BigDecimal price;
    private String link;
}
