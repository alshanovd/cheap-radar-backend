package com.cheapradar.backend.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class ProviderAggregateResult {
    private List<ProviderTicket> tickets;
    private Set<String> successfulProviders;
    private Set<String> failedProviders;
}
