package com.cheapradar.backend.service;

import com.cheapradar.backend.dto.settings.SettingsRequest;
import com.cheapradar.backend.dto.settings.SettingsResponse;
import com.cheapradar.backend.model.Setting;
import com.cheapradar.backend.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository repository;

    public SettingsResponse getSettings(BigInteger userId) {
        Optional<Setting> optional = repository.getByUserId(userId);
        if (optional.isEmpty()) {
            return new SettingsResponse("USD", true, "dark");
        }
        Setting setting = optional.get();
        return new SettingsResponse(setting.getCurrency(), setting.isNotifications(), setting.getTheme());
    }

    public SettingsResponse saveSettings(SettingsRequest request) {
        BigInteger userId = request.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required");
        }

        Optional<Setting> optional = repository.getByUserId(userId);
        Setting setting;
        if (optional.isEmpty()) {
            setting = new Setting();
            setting.setUserId(userId);
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

        Setting savedSetting = repository.save(setting);
        return new SettingsResponse(savedSetting.getCurrency(), savedSetting.isNotifications(), savedSetting.getTheme());
    }
}
