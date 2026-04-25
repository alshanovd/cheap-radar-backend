package com.cheapradar.backend.client.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ClientSearchRequest {
    private String airportFrom;
    private String airportTo;
    private LocalDate dateFrom;
    private LocalDate dateTo;

}
