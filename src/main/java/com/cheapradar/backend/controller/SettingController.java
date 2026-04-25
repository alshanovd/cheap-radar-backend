package com.cheapradar.backend.controller;

import com.cheapradar.backend.dto.settings.SettingsRequest;
import com.cheapradar.backend.dto.settings.SettingsResponse;
import com.cheapradar.backend.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigInteger;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(
            @RequestParam(value = "user_id", defaultValue = "1") BigInteger userId
    ) {
        log.info("Received request to get settings for user: {}", userId);
        SettingsResponse response = settingService.getSettings(userId);
        log.info("Returning settings: {}", response);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<SettingsResponse> saveSettings(@RequestBody SettingsRequest settings) {
        log.info("Received request to save settings: {}", settings);
        SettingsResponse response = settingService.saveSettings(settings);
        log.info("Returning settings: {}", response);
        return ResponseEntity.ok(response);
    }
}
