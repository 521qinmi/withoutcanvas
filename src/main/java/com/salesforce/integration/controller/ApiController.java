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
@CrossOrigin(origins = {
    "https://bigdipper-pluto-4490.scratch.lightning.force.com",
    "https://bigdipper-pluto-4490.scratch.my.salesforce.com"
})
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    private final SalesforceApiService salesforceApiService;
    private final SalesforceOAuthClient oauthClient;
    
    public ApiController(SalesforceApiService salesforceApiService, 
                        SalesforceOAuthClient oauthClient) {
        this.salesforceApiService = salesforceApiService;
        this.oauthClient = oauthClient;
    }
    
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        try {
            logger.info("Get Account Info: {}", id);
            JsonNode account = salesforceApiService.getAccountById(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            logger.error("Get Account Failure", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/task")
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> taskData) {
        try {
            logger.info("Create Task: {}", taskData);
            
            String jsonBody = String.format(
                "{\"Subject\":\"%s\", \"WhatId\":\"%s\", \"Status\":\"%s\"}",
                taskData.get("subject"),
                taskData.get("whatId"),
                taskData.get("status")
            );
            
            JsonNode result = salesforceApiService.createRecord("Task", jsonBody);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Create Task Failure", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PatchMapping("/account/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable String id, 
                                          @RequestBody Map<String, Object> updateData) {
        try {
            logger.info("Update Account: {}, data: {}", id, updateData);
            
            // 构建更新字段
            StringBuilder fields = new StringBuilder("{");
            for (Map.Entry<String, Object> entry : updateData.entrySet()) {
                if (fields.length() > 1) fields.append(",");
                fields.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
            }
            fields.append("}");
            
            JsonNode result = salesforceApiService.updateRecord("Account", id, fields.toString());
            return ResponseEntity.ok(Map.of("success", true, "id", id));
        } catch (Exception e) {
            logger.error("Update Account Failure", e);
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
        status.put("version", "1.0.0");
        
        try {
            // 测试连接
            oauthClient.getAccessToken();
            status.put("salesforce", "connected");
        } catch (Exception e) {
            status.put("salesforce", "disconnected");
            status.put("salesforceError", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }
}

