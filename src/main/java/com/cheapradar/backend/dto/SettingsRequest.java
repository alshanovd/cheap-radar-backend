package com.cheapradar.backend.dto;


import lombok.Data;

@Data
public class SettingsRequest {
    private String currency;
    private Boolean notifications;
    private String theme;
}
