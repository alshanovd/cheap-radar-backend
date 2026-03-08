package com.cheapradar.backend.controller;

import com.cheapradar.backend.model.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<?> createSearch(@RequestBody Map<String, Object> request) {
        // Mock creating a new search
        Long mockedSearchId = (long) (Math.random() * 1000);
        
        // Simulating immediate websocket push for this search ID as a mock
        // Example: someone creates a search, we send an initial mocked data back via websocket
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                messagingTemplate.convertAndSend("/topic/search/" + mockedSearchId, 
                    Map.of("status", "processing", "message", "Found 3 new flights..."));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok(Map.of("id", mockedSearchId, "status", "Search initiated"));
    }

    @GetMapping
    public ResponseEntity<List<Search>> getOngoingSearches() {
        // Mock list of active searches
        return ResponseEntity.ok(List.of(
                new Search("JFK", "LHR", LocalDateTime.now(), "ONGOING"),
                new Search("DXB", "LAX", LocalDateTime.now().plusHours(1), "ONGOING"),
                new Search("SYD", "SFO", LocalDateTime.now().plusDays(2), "ONGOING")
        ));
    }
}
