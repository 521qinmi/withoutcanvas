package com.salesforce.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.salesforce.integration.service.SalesforceApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    private final SalesforceApiService salesforceApiService;
    
    public ApiController(SalesforceApiService salesforceApiService) {
        this.salesforceApiService = salesforceApiService;
    }
    
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        try {
            JsonNode account = salesforceApiService.getAccountById(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));
    }
}