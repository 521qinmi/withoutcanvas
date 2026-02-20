package com.salesforce.integration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
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
    
    /*@GetMapping("/account/{id}")
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
    }*/
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        logger.info("========== GET ACCOUNT REQUEST ==========");
        logger.info("Account ID: {}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 尝试获取token
            logger.info("Step 1: Getting access token...");
            var token = oauthClient.getAccessToken();
            logger.info("Step 1 Complete - Instance URL: {}", token.getInstanceUrl());
            
            // 尝试获取account
            logger.info("Step 2: Fetching account from Salesforce...");
            Map<String, Object> account = salesforceApiService.getAccountById(id);
            logger.info("Step 2 Complete - Account found: {}", account.get("Name"));
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Total time: {}ms", duration);
            
            return ResponseEntity.ok(account);
            
        } catch (Exception e) {
            logger.error("❌ ERROR in getAccount:", e);
            
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            response.put("accountId", id);
            response.put("timestamp", System.currentTimeMillis());
            
            // 如果是HTTP错误，尝试提取更多信息
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                response.put("httpStatus", httpEx.getStatusCode().value());
                response.put("httpResponse", httpEx.getResponseBodyAsString());
            }
            
            return ResponseEntity.status(500).body(response);
        } finally {
            logger.info("========== END REQUEST ==========");
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
            
            String jsonBody = String.format(
                "{\"WhatId\":\"%s\", \"Subject\":\"%s\", \"Status\":\"%s\"}",
                whatId, subject, status != null ? status : "Not Started"
            );
            
            JsonNode result = salesforceApiService.createRecord("Task", jsonBody);
            
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
            
            // 构建更新字段
            StringBuilder fields = new StringBuilder("{");
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                if (fields.length() > 1) fields.append(",");
                fields.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
            }
            fields.append("}");
            
            JsonNode result = salesforceApiService.updateRecord("Account", id, fields.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", id);
            response.put("updated", updates.keySet());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating account", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken() {
        try {
            oauthClient.clearTokenCache();
            var token = oauthClient.getAccessToken();
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
        status.put("service", "salesforce-java-app");
        status.put("version", "1.0.0");
        
        try {
            // Test connection
            oauthClient.getAccessToken();
            status.put("salesforce", "connected");
        } catch (Exception e) {
            status.put("salesforce", "disconnected");
            status.put("salesforceError", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
}

