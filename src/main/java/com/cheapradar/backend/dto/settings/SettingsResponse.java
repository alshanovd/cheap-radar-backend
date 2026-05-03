package com.cheapradar.backend.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettingsResponse {
    private String currency;
    private boolean notifications;
}
