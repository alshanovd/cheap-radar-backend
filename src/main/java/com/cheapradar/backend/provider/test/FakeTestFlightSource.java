package com.cheapradar.backend.provider.test;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class FakeTestFlightSource {
    public List<TestFlightOffer> search(ProviderSearchRequest request) {
        return List.of(
                offer(request, 0, 0, 10000, "Jetstar", "JQ"),
                offer(request, 0, 14, 12400, "Virgin Australia", "VA"),
                offer(request, 0, 20, 13200, "Qantas", "QF")
        );
    }

    private TestFlightOffer offer(ProviderSearchRequest request, int days, int hour, long costCents,
                                  String carrierName, String carrierCode) {
        LocalDateTime departureTime = request.getDateFrom().plusDays(days).atTime(hour, 25);

        return TestFlightOffer.builder()
                .originCode(request.getAirportFrom())
                .destinationCode(request.getAirportTo())
                .departureEpochSeconds(departureTime.toEpochSecond(ZoneOffset.UTC))
                .costCents(costCents)
                .bookingUri("https://www.example.com/ticket")
                .carrierName(carrierName)
                .carrierImageUri("https://www.gstatic.com/flights/airline_logos/70px/" + carrierCode + ".png")
                .build();
    }
}
