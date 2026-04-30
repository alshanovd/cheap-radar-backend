package com.cheapradar.backend.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ProviderSearchRequest {
    private String airportFrom;
    private String airportTo;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}
