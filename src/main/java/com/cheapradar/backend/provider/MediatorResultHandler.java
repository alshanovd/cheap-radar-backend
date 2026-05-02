package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderTicket;

import java.time.LocalDate;
import java.util.List;

@FunctionalInterface
public interface MediatorResultHandler {
    void onSuccess(String providerSlug, LocalDate date, List<ProviderTicket> tickets);
}
