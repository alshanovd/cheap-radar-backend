package com.cheapradar.backend.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class MediatorSearchResult {
    private List<ProviderTicket> tickets;
    private Set<String> successfulProviders;
    private Set<String> failedProviders;
    private Set<ProviderDateResult> successfulProviderDates;
    private Set<ProviderDateResult> failedProviderDates;

    public record ProviderDateResult(String providerSlug, LocalDate date) {
    }
}
