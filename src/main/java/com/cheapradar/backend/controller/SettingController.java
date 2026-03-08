package com.cheapradar.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/setting")
@CrossOrigin(origins = "*")
public class SettingController {

    @GetMapping
    public ResponseEntity<?> getSettings() {
        // Mock settings object
        return ResponseEntity.ok(Map.of(
                "theme", "dark",
                "notifications", true,
                "currency", "USD"
        ));
    }

    @PostMapping
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, Object> settings) {
        // Mock save
        return ResponseEntity.ok(Map.of("message", "Settings updated successfully", "saved", settings));
    }
}
