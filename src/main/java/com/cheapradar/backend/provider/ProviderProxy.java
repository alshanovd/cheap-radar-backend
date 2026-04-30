package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderAggregateResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProviderProxy {
    private static final String FALLBACK_PROVIDER = "test";

    private final Map<String, FlightProvider> providers;

    public ProviderProxy(List<FlightProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(provider -> normalize(provider.slug()), Function.identity()));
    }

    public ProviderAggregateResult search(List<String> providerSlugs, ProviderSearchRequest request) {
        Set<String> requestedSlugs = providerSlugs == null
                ? Set.of()
                : providerSlugs.stream()
                        .filter(Objects::nonNull)
                        .map(this::normalize)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CompletableFuture<ProviderCallResult>> futures = requestedSlugs.stream()
                .map(requestedSlug -> CompletableFuture.supplyAsync(() -> searchProvider(requestedSlug, request)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ProviderCallResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return ProviderAggregateResult.builder()
                .tickets(results.stream()
                        .flatMap(result -> result.tickets().stream())
                        .sorted()
                        .toList())
                .successfulProviders(results.stream()
                        .filter(ProviderCallResult::success)
                        .map(ProviderCallResult::requestedSlug)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .failedProviders(results.stream()
                        .filter(result -> !result.success())
                        .map(ProviderCallResult::requestedSlug)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }

    private ProviderCallResult searchProvider(String requestedSlug, ProviderSearchRequest request) {
        FlightProvider provider = providers.get(requestedSlug);
        if (provider == null) {
            provider = providers.get(FALLBACK_PROVIDER);
            log.warn("Unknown provider '{}', falling back to '{}'", requestedSlug, FALLBACK_PROVIDER);
        }

        if (provider == null) {
            log.error("Fallback provider '{}' is not configured", FALLBACK_PROVIDER);
            return ProviderCallResult.failed(requestedSlug);
        }

        try {
            ProviderSearchResponse response = provider.search(request);
            List<ProviderTicket> tickets = response == null || response.getTickets() == null
                    ? List.of()
                    : new ArrayList<>(response.getTickets());
            return ProviderCallResult.succeeded(requestedSlug, tickets);
        } catch (RuntimeException exception) {
            log.error("Provider '{}' failed while handling requested provider '{}'",
                    provider.slug(), requestedSlug, exception);
            return ProviderCallResult.failed(requestedSlug);
        }
    }

    private String normalize(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private record ProviderCallResult(String requestedSlug, boolean success, List<ProviderTicket> tickets) {
        private static ProviderCallResult succeeded(String requestedSlug, List<ProviderTicket> tickets) {
            return new ProviderCallResult(requestedSlug, true, tickets);
        }

        private static ProviderCallResult failed(String requestedSlug) {
            return new ProviderCallResult(requestedSlug, false, List.of());
        }
    }
}
