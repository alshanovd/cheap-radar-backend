package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;

public interface FlightProvider {
    String slug();

    ProviderSearchResponse search(ProviderSearchRequest request);
}
