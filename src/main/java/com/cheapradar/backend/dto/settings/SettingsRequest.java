package com.cheapradar.backend.dto.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigInteger;

@Data
public class SettingsRequest {
    @JsonProperty("user_id")
    private BigInteger userId;
    private String currency;
    private Boolean notifications;
}
