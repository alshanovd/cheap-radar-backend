package com.cheapradar.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mock")
public class MockController {

    @GetMapping
    public Map<String, Object> getMockData() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "success");
        data.put("message", "App is deployed and running!");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
}
