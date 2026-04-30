package com.cheapradar.backend.provider.test;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestFlightOffer {
    private String originCode;
    private String destinationCode;
    private long departureEpochSeconds;
    private long costCents;
    private String bookingUri;
    private String carrierName;
    private String carrierImageUri;
}
