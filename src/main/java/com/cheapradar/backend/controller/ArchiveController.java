package com.cheapradar.backend.controller;

import com.cheapradar.backend.model.Search;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/archive")
@CrossOrigin(origins = "*")
public class ArchiveController {

    @GetMapping
    public ResponseEntity<List<Search>> getArchivedSearches() {
        // Mock list of historical searches
        return ResponseEntity.ok(List.of(
                new Search("CDG", "JFK", LocalDateTime.now().minusDays(5), "ARCHIVED"),
                new Search("MIA", "EZE", LocalDateTime.now().minusDays(10), "ARCHIVED"),
                new Search("NRT", "HNL", LocalDateTime.now().minusDays(12), "COMPLETED"),
                new Search("CDG", "JFK", LocalDateTime.now().minusDays(5), "ARCHIVED"),
                new Search("MIA", "EZE", LocalDateTime.now().minusDays(10), "ARCHIVED"),
                new Search("NRT", "HNL", LocalDateTime.now().minusDays(12), "COMPLETED"),
                new Search("CDG", "JFK", LocalDateTime.now().minusDays(5), "ARCHIVED"),
                new Search("MIA", "EZE", LocalDateTime.now().minusDays(10), "ARCHIVED"),
                new Search("NRT", "HNL", LocalDateTime.now().minusDays(12), "COMPLETED"),
                new Search("CDG", "JFK", LocalDateTime.now().minusDays(5), "ARCHIVED"),
                new Search("MIA", "EZE", LocalDateTime.now().minusDays(10), "ARCHIVED"),
                new Search("NRT", "HNL", LocalDateTime.now().minusDays(12), "COMPLETED")
        ));
    }
}
