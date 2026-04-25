package com.cheapradar.backend.client;

import com.cheapradar.backend.client.dto.ClientSearchRequest;
import com.cheapradar.backend.client.dto.ClientSearchResponse;

public interface SearchClient {
    ClientSearchResponse search(ClientSearchRequest request);
}
