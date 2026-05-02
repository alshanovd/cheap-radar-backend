package com.cheapradar.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSearchRequest {
    @JsonProperty(required = true)
    private String airportFrom;

    @JsonProperty(required = true)
    private String airportTo;

    @JsonProperty(required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @JsonProperty(required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    @JsonProperty(required = true)
    private Integer checkIntervalHours;

    @JsonProperty(required = true)
    private Integer checkCount;

    @JsonProperty(required = true)
    private List<String> providers;
}
