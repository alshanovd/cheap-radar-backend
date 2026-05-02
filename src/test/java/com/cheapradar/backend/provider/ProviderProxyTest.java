package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderAggregateResult;
import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderProxyTest {
    private final ProviderSearchRequest request = ProviderSearchRequest.builder()
            .airportFrom("SYD")
            .airportTo("MEL")
            .dateFrom(LocalDate.of(2026, 5, 1))
            .dateTo(LocalDate.of(2026, 5, 1))
            .build();

    @Test
    void normalizesRequestedProviderSlugs() {
        ProviderProxy proxy = new ProviderProxy(List.of(new StubProvider("google")));

        ProviderAggregateResult result = proxy.search(List.of("GOOGLE"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void fallsBackUnknownProviderToTestProvider() {
        ProviderProxy proxy = new ProviderProxy(List.of(new StubProvider("test")));

        ProviderAggregateResult result = proxy.search(List.of("unknown"), request);

        assertEquals(List.of("test"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("unknown"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void keepsSuccessfulProviderResultsWhenAnotherProviderFails() {
        ProviderProxy proxy = new ProviderProxy(List.of(
                new StubProvider("google"),
                new FailingProvider("test")
        ));

        ProviderAggregateResult result = proxy.search(List.of("google", "test"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertEquals(List.of("test"), result.getFailedProviders().stream().toList());
    }

    @Test
    void notifiesSuccessfulProviderBeforeAllProvidersFinish() throws Exception {
        CountDownLatch slowProviderRelease = new CountDownLatch(1);
        CountDownLatch callbackReceived = new CountDownLatch(1);
        ProviderProxy proxy = new ProviderProxy(List.of(
                new StubProvider("google"),
                new BlockingProvider("test", slowProviderRelease)
        ));

        CompletableFuture<ProviderAggregateResult> result = CompletableFuture.supplyAsync(() ->
                proxy.search(List.of("google", "test"), request, (providerSlug, tickets) -> {
                    if ("google".equals(providerSlug)) {
                        callbackReceived.countDown();
                    }
                }));

        assertTrue(callbackReceived.await(1, TimeUnit.SECONDS));
        assertFalse(result.isDone());

        slowProviderRelease.countDown();

        assertEquals(List.of("google", "test"), result.get().getSuccessfulProviders().stream().toList());
    }

    @Test
    void doesNotNotifyFailedProviders() {
        List<String> successfulCallbacks = new ArrayList<>();
        ProviderProxy proxy = new ProviderProxy(List.of(
                new StubProvider("google"),
                new FailingProvider("test")
        ));

        ProviderAggregateResult result = proxy.search(List.of("google", "test"), request,
                (providerSlug, tickets) -> successfulCallbacks.add(providerSlug));

        assertEquals(List.of("google"), successfulCallbacks);
        assertEquals(List.of("test"), result.getFailedProviders().stream().toList());
    }

    private static class StubProvider implements FlightProvider {
        private final String slug;

        private StubProvider(String slug) {
            this.slug = slug;
        }

        @Override
        public String slug() {
            return slug;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            return ProviderSearchResponse.builder()
                    .tickets(List.of(ProviderTicket.builder()
                            .provider(slug)
                            .airportFrom(request.getAirportFrom())
                            .airportTo(request.getAirportTo())
                            .date(LocalDateTime.of(2026, 5, 1, 12, 0))
                            .price(BigDecimal.TEN)
                            .build()))
                    .build();
        }
    }

    private static class FailingProvider implements FlightProvider {
        private final String slug;

        private FailingProvider(String slug) {
            this.slug = slug;
        }

        @Override
        public String slug() {
            return slug;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            throw new RuntimeException("provider unavailable");
        }
    }

    private static class BlockingProvider extends StubProvider {
        private final CountDownLatch release;

        private BlockingProvider(String slug, CountDownLatch release) {
            super(slug);
            this.release = release;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            return super.search(request);
        }
    }
}
