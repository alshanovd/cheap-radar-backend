package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.MediatorSearchResult;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightSearchMediatorTest {
    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_2 = LocalDate.of(2026, 5, 2);
    private final ProviderSearchRequest request = request(MAY_1, MAY_1);

    @Test
    void normalizesRequestedProviderSlugs() {
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(new StubProvider("google")));

        MediatorSearchResult result = mediator.search(List.of("GOOGLE"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void fallsBackUnknownProviderToTestProvider() {
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(new StubProvider("test")));

        MediatorSearchResult result = mediator.search(List.of("unknown"), request);

        assertEquals(List.of("test"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("unknown"), result.getSuccessfulProviders().stream().toList());
        assertTrue(result.getFailedProviders().isEmpty());
    }

    @Test
    void keepsSuccessfulProviderResultsWhenAnotherProviderFails() {
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(
                new StubProvider("google"),
                new FailingProvider("test")
        ));

        MediatorSearchResult result = mediator.search(List.of("google", "test"), request);

        assertEquals(List.of("google"), result.getTickets().stream().map(ProviderTicket::getProvider).toList());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertEquals(List.of("test"), result.getFailedProviders().stream().toList());
    }

    @Test
    void notifiesSuccessfulProviderBeforeAllProvidersFinish() throws Exception {
        CountDownLatch slowProviderRelease = new CountDownLatch(1);
        CountDownLatch callbackReceived = new CountDownLatch(1);
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(
                new StubProvider("google"),
                new BlockingProvider("test", slowProviderRelease)
        ));

        CompletableFuture<MediatorSearchResult> result = CompletableFuture.supplyAsync(() ->
                mediator.search(List.of("google", "test"), request, (providerSlug, date, tickets) -> {
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
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(
                new StubProvider("google"),
                new FailingProvider("test")
        ));

        MediatorSearchResult result = mediator.search(List.of("google", "test"), request,
                (providerSlug, date, tickets) -> successfulCallbacks.add(providerSlug));

        assertEquals(List.of("google"), successfulCallbacks);
        assertEquals(List.of("test"), result.getFailedProviders().stream().toList());
    }

    @Test
    void searchesEveryDateWithSingleDayProviderRequests() {
        RecordingProvider google = new RecordingProvider("google");
        RecordingProvider test = new RecordingProvider("test");
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(google, test));

        MediatorSearchResult result = mediator.search(List.of("google", "test"), request(MAY_1, MAY_2));

        assertEquals(List.of(MAY_1, MAY_2), google.requestDates());
        assertEquals(List.of(MAY_1, MAY_2), test.requestDates());
        assertTrue(google.requests.stream()
                .allMatch(request -> request.getDateFrom().equals(request.getDateTo())));
        assertEquals(List.of("google", "test"), result.getSuccessfulProviders().stream().toList());
        assertEquals(List.of(
                "google:2026-05-01",
                "google:2026-05-02",
                "test:2026-05-01",
                "test:2026-05-02"
        ), result.getSuccessfulProviderDates().stream().map(this::providerDate).toList());
    }

    @Test
    void tracksProviderDateFailuresSeparatelyFromProviderSummary() {
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(
                new DateFailingProvider("google", MAY_2)
        ));

        MediatorSearchResult result = mediator.search(List.of("google"), request(MAY_1, MAY_2));

        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
        assertEquals(List.of("google"), result.getFailedProviders().stream().toList());
        assertEquals(List.of("google:2026-05-01"),
                result.getSuccessfulProviderDates().stream().map(this::providerDate).toList());
        assertEquals(List.of("google:2026-05-02"),
                result.getFailedProviderDates().stream().map(this::providerDate).toList());
    }

    @Test
    void filtersTicketsThatDoNotMatchRequestedDate() {
        FlightSearchMediator mediator = new FlightSearchMediator(List.of(
                new MismatchedDateProvider("google", MAY_2)
        ));

        MediatorSearchResult result = mediator.search(List.of("google"), request(MAY_1, MAY_1));

        assertTrue(result.getTickets().isEmpty());
        assertEquals(List.of("google"), result.getSuccessfulProviders().stream().toList());
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
                            .date(request.getDateFrom().atTime(12, 0))
                            .price(BigDecimal.TEN)
                            .build()))
                    .build();
        }
    }

    private static class RecordingProvider extends StubProvider {
        private final List<ProviderSearchRequest> requests = new CopyOnWriteArrayList<>();

        private RecordingProvider(String slug) {
            super(slug);
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            requests.add(request);
            return super.search(request);
        }

        private List<LocalDate> requestDates() {
            return requests.stream()
                    .map(ProviderSearchRequest::getDateFrom)
                    .sorted()
                    .toList();
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

    private static class DateFailingProvider extends StubProvider {
        private final LocalDate failingDate;

        private DateFailingProvider(String slug, LocalDate failingDate) {
            super(slug);
            this.failingDate = failingDate;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            if (failingDate.equals(request.getDateFrom())) {
                throw new RuntimeException("provider unavailable");
            }
            return super.search(request);
        }
    }

    private static class MismatchedDateProvider extends StubProvider {
        private final LocalDate ticketDate;

        private MismatchedDateProvider(String slug, LocalDate ticketDate) {
            super(slug);
            this.ticketDate = ticketDate;
        }

        @Override
        public ProviderSearchResponse search(ProviderSearchRequest request) {
            return ProviderSearchResponse.builder()
                    .tickets(List.of(ProviderTicket.builder()
                            .provider(slug())
                            .airportFrom(request.getAirportFrom())
                            .airportTo(request.getAirportTo())
                            .date(ticketDate.atTime(12, 0))
                            .price(BigDecimal.TEN)
                            .build()))
                    .build();
        }
    }

    private ProviderSearchRequest request(LocalDate dateFrom, LocalDate dateTo) {
        return ProviderSearchRequest.builder()
                .airportFrom("SYD")
                .airportTo("MEL")
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();
    }

    private String providerDate(MediatorSearchResult.ProviderDateResult result) {
        return result.providerSlug() + ":" + result.date();
    }
}
