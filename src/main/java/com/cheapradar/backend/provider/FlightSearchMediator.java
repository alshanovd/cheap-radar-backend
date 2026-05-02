package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.MediatorSearchResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
public class FlightSearchMediator {
    private static final String FALLBACK_PROVIDER = "test";

    private final Map<String, FlightProvider> providers;

    public FlightSearchMediator(List<FlightProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(provider -> normalize(provider.slug()), Function.identity()));
    }

    public MediatorSearchResult search(List<String> providerSlugs, ProviderSearchRequest request) {
        return search(providerSlugs, request, (providerSlug, date, tickets) -> {
        });
    }

    public MediatorSearchResult search(List<String> providerSlugs,
                                       ProviderSearchRequest request,
                                       MediatorResultHandler resultHandler) {
        Set<String> requestedSlugs = providerSlugs == null
                ? Set.of()
                : providerSlugs.stream()
                        .filter(Objects::nonNull)
                        .map(this::normalize)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CompletableFuture<ProviderCallResult>> futures = requestedSlugs.stream()
                .flatMap(requestedSlug -> dates(request).stream()
                        .map(date -> CompletableFuture.supplyAsync(() -> searchProvider(requestedSlug, request, date))
                                .thenApply(result -> handleProviderResult(result, resultHandler))
                                .exceptionally(exception -> {
                                    log.error("Provider '{}' result handling failed for date {}",
                                            requestedSlug, date, exception);
                                    return ProviderCallResult.failed(requestedSlug, date);
                                })))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ProviderCallResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return MediatorSearchResult.builder()
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
                .successfulProviderDates(results.stream()
                        .filter(ProviderCallResult::success)
                        .map(result -> new MediatorSearchResult.ProviderDateResult(
                                result.requestedSlug(), result.date()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .failedProviderDates(results.stream()
                        .filter(result -> !result.success())
                        .map(result -> new MediatorSearchResult.ProviderDateResult(
                                result.requestedSlug(), result.date()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }

    private ProviderCallResult handleProviderResult(ProviderCallResult result, MediatorResultHandler resultHandler) {
        if (result.success()) {
            resultHandler.onSuccess(result.requestedSlug(), result.date(), result.tickets());
        }
        return result;
    }

    private ProviderCallResult searchProvider(String requestedSlug, ProviderSearchRequest request, LocalDate date) {
        FlightProvider provider = providers.get(requestedSlug);
        if (provider == null) {
            provider = providers.get(FALLBACK_PROVIDER);
            log.warn("Unknown provider '{}', falling back to '{}'", requestedSlug, FALLBACK_PROVIDER);
        }

        if (provider == null) {
            log.error("Fallback provider '{}' is not configured", FALLBACK_PROVIDER);
            return ProviderCallResult.failed(requestedSlug, date);
        }

        try {
            ProviderSearchResponse response = provider.search(singleDayRequest(request, date));
            List<ProviderTicket> tickets = response == null || response.getTickets() == null
                    ? List.of()
                    : new ArrayList<>(response.getTickets()).stream()
                            .filter(ticket -> ticket.getDate() != null && date.equals(ticket.getDate().toLocalDate()))
                            .toList();
            return ProviderCallResult.succeeded(requestedSlug, date, tickets);
        } catch (RuntimeException exception) {
            log.error("Provider '{}' failed while handling requested provider '{}'",
                    provider.slug(), requestedSlug, exception);
            return ProviderCallResult.failed(requestedSlug, date);
        }
    }

    private List<LocalDate> dates(ProviderSearchRequest request) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = request.getDateFrom();
        LocalDate end = request.getDateTo();

        while (!date.isAfter(end)) {
            dates.add(date);
            date = date.plusDays(1);
        }

        return dates;
    }

    private ProviderSearchRequest singleDayRequest(ProviderSearchRequest request, LocalDate date) {
        return ProviderSearchRequest.builder()
                .airportFrom(request.getAirportFrom())
                .airportTo(request.getAirportTo())
                .dateFrom(date)
                .dateTo(date)
                .build();
    }

    private String normalize(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private record ProviderCallResult(String requestedSlug, LocalDate date, boolean success, List<ProviderTicket> tickets) {
        private static ProviderCallResult succeeded(String requestedSlug, LocalDate date, List<ProviderTicket> tickets) {
            return new ProviderCallResult(requestedSlug, date, true, tickets);
        }

        private static ProviderCallResult failed(String requestedSlug, LocalDate date) {
            return new ProviderCallResult(requestedSlug, date, false, List.of());
        }
    }
}
