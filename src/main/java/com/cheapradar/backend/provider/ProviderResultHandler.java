package com.cheapradar.backend.provider;

import com.cheapradar.backend.provider.dto.ProviderTicket;

import java.util.List;

@FunctionalInterface
public interface ProviderResultHandler {
    void onSuccess(String providerSlug, List<ProviderTicket> tickets);
}
