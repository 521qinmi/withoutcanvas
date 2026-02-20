package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    private final SalesforceApiService salesforceApiService;
    private final SalesforceOAuthClient oauthClient;
    
    public ApiController(SalesforceApiService salesforceApiService, SalesforceOAuthClient oauthClient) {
        this.salesforceApiService = salesforceApiService;
        this.oauthClient = oauthClient;
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Java API is working");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", "railway");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/estimate/{id}")
    public ResponseEntity<?> getEstimate(@PathVariable String id) {
        try {
            logger.info("Getting estimate from Salesforce: {}", id);
            Map<String, Object> estimate = salesforceApiService.getEstimateById(id);
            return ResponseEntity.ok(estimate);
        } catch (Exception e) {
            logger.error("Error getting account: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("estimateId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        try {
            logger.info("Getting account from Salesforce: {}", id);
            Map<String, Object> account = salesforceApiService.getAccountById(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            logger.error("Error getting account: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("accountId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody Map<String, String> taskData) {
        try {
            String whatId = taskData.get("whatId");
            String subject = taskData.get("subject");
            String status = taskData.get("status");
            
            if (whatId == null || subject == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "whatId and subject are required"));
            }
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("WhatId", whatId);
            fields.put("Subject", subject);
            fields.put("Status", status != null ? status : "Not Started");
            fields.put("Priority", "Normal");
            
            var result = salesforceApiService.createRecord("Task", fields);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.get("id").asText());
            response.put("success", true);
            response.put("whatId", whatId);
            response.put("subject", subject);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating task", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PatchMapping("/account/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
            logger.info("Updating account {} with: {}", id, updates);
            
            Map<String, Object> result = salesforceApiService.updateAccount(id, updates);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error updating account", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken() {
        try {
            oauthClient.clearTokenCache();
            TokenInfo token = oauthClient.getAccessToken();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "expiresIn", token.getExpiresIn()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        status.put("service", "salesforce-java-integration");
        status.put("version", "1.0.0");
        
        try {
            TokenInfo token = oauthClient.getAccessToken();
            status.put("salesforce", "connected");
            status.put("instance_url", token.getInstanceUrl());
        } catch (Exception e) {
            status.put("salesforce", "disconnected");
            status.put("salesforce_error", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
}


