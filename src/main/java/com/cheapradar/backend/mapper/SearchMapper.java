package com.cheapradar.backend.mapper;

import com.cheapradar.backend.dto.search.CreateSearchRequest;
import com.cheapradar.backend.model.Search;
import org.springframework.stereotype.Component;

@Component
public class SearchMapper {
    public Search map(CreateSearchRequest request) {
        return new Search(
                request.getAirportFrom(),
                request.getAirportTo(),
                request.getDateFrom(),
                request.getDateTo(),
                request.getCheckIntervalHours(),
                request.getCheckCount(),
                request.getProviders()
        );
    }
}
