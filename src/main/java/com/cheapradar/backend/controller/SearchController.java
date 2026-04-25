package com.cheapradar.backend.controller;

import com.cheapradar.backend.dto.search.CreateSearchRequest;
import com.cheapradar.backend.dto.search.CreateSearchResponse;
import com.cheapradar.backend.dto.search.SearchResultsResponse;
import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {
    private final SearchService service;

    @PostMapping
    public ResponseEntity<CreateSearchResponse> createSearch(@RequestBody CreateSearchRequest request) {
        Search search = service.createSearch(request);
        service.updateSearchResults(search);
        CreateSearchResponse response = new CreateSearchResponse(search.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{searchId}")
    public ResponseEntity<SearchResultsResponse> getSearchResults(@PathVariable String searchId) {
        SearchResultsResponse response = service.getSearchResults(searchId);
        return ResponseEntity.ok(response);
    }
}
