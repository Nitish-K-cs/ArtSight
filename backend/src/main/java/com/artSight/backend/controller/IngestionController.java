package com.artSight.backend.controller;

import com.artSight.backend.service.MetIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ingest")
public class IngestionController {

    @Autowired
    private MetIngestionService ingestionService;

    @PostMapping
    public String ingest(@RequestParam String query, @RequestParam(defaultValue = "50") int limit) {
        int count = ingestionService.ingestByQuery(query, limit);
        return "Ingested " + count + " artworks for query: " + query;
    }
}