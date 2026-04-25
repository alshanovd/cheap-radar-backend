package com.cheapradar.backend.dto.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetAllSearchesResponse {
    private List<SearchResultsResponse> searches;
}
