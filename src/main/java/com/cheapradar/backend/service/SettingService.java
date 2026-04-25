package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.settings.SettingsRequest;
import com.cheapradar.backend.dto.settings.SettingsResponse;
import com.cheapradar.backend.model.Setting;
import com.cheapradar.backend.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository repository;

    public SettingsResponse getSettings() {
        Optional<Setting> optional = repository.getByUserId(BigInteger.ONE);
        if (optional.isEmpty()) {
            return new SettingsResponse("USD", true, "dark");
        }
        Setting setting = optional.get();
        return new SettingsResponse(setting.getCurrency(), setting.isNotifications(), setting.getTheme());
    }

    public SettingsResponse saveSettings(SettingsRequest request) {
        Optional<Setting> optional = repository.getByUserId(BigInteger.ONE);
        Setting setting;
        if (optional.isEmpty()) {
            setting = new Setting();
            setting.setUserId(BigInteger.ONE);
        } else {
            setting = optional.get();
        }

        if (request.getCurrency() != null) {
            setting.setCurrency(request.getCurrency());
        }
        if (request.getNotifications() != null) {
            setting.setNotifications(request.getNotifications());
        }
        if (request.getTheme() != null) {
            setting.setTheme(request.getTheme());
        }

        repository.save(setting);
        return new SettingsResponse(setting.getCurrency(), setting.isNotifications(), setting.getTheme());
    }
}
