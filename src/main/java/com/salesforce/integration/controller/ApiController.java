package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.model.TokenInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    private final SalesforceApiService salesforceApiService;
    private final SalesforceOAuthClient oauthClient;
    
    @Value("${salesforce.oauth.api-version:v59.0}")
    private String apiVersion;
    
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
    
    @GetMapping("/auth/test")
    public ResponseEntity<?> testAuth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            TokenInfo tokenInfo = oauthClient.getAccessToken();
            
            result.put("success", true);
            result.put("instance_url", tokenInfo.getInstanceUrl());
            result.put("token_type", tokenInfo.getTokenType());
            result.put("expires_in", tokenInfo.getExpiresIn());
            result.put("issued_at", tokenInfo.getIssuedAt());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(result);
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
    
    @GetMapping("/estimate/{id}")
    public ResponseEntity<?> getEstimate(@PathVariable String id) {
        try {
            logger.info("Getting estimate from Salesforce: {}", id);
            Map<String, Object> estimate = salesforceApiService.getEstimateById(id);
            return ResponseEntity.ok(estimate);
        } catch (Exception e) {
            logger.error("Error getting estimate: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("estimateId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/record/{id}")
    public ResponseEntity<?> getRecord(@PathVariable String id) {
        try {
            logger.info("Getting record: {}", id);
            
            // 根据ID前缀推断对象类型
            String objectType = inferObjectTypeFromId(id);
            logger.info("Inferred object type: {}", objectType);
            
            Map<String, Object> record = salesforceApiService.getRecordById(objectType, id);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.error("Error getting record: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("recordId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/debug/check-object/{id}")
    public ResponseEntity<?> checkObjectName(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", id);
        
        try {
            TokenInfo token = oauthClient.getAccessToken();
            
            // 尝试不同的对象名变体
            String[] possibleNames = {
                "ffscpq_Estimate__c",
                "ffscpq_Estimate_c",
                "Estimate__c",
                "ffscpq_Estimate"
            };
            
            Map<String, Object> attempts = new HashMap<>();
            RestTemplate restTemplate = new RestTemplate();
            
            for (String objectName : possibleNames) {
                try {
                    String soql = "SELECT Id FROM " + objectName + " WHERE Id = '" + id + "' LIMIT 1";
                    
                    String url = UriComponentsBuilder.fromHttpUrl(token.getInstanceUrl())
                            .path("/services/data/" + apiVersion + "/query")
                            .queryParam("q", soql)
                            .build()
                            .toUriString();
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token.getAccessToken());
                    
                    HttpEntity<String> request = new HttpEntity<>(headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                    
                    attempts.put(objectName, Map.of(
                        "success", true,
                        "status", response.getStatusCode().value(),
                        "response", response.getBody()
                    ));
                    
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                        org.springframework.web.client.HttpClientErrorException httpEx = 
                            (org.springframework.web.client.HttpClientErrorException) e;
                        errorMsg = httpEx.getResponseBodyAsString();
                    }
                    
                    attempts.put(objectName, Map.of(
                        "success", false,
                        "error", errorMsg
                    ));
                }
            }
            
            result.put("attempts", attempts);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/sobject/{objectName}/{id}")
    public ResponseEntity<?> getSObject(@PathVariable String objectName, @PathVariable String id) {
        try {
            logger.info("Getting {}/{}", objectName, id);
            
            TokenInfo token = oauthClient.getAccessToken();
            
            String url = token.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/" + objectName + "/" + id;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode record = mapper.readTree(response.getBody());
            
            return ResponseEntity.ok(record);
            
        } catch (Exception e) {
            logger.error("Error: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/query")
    public ResponseEntity<?> debugQuery(@RequestParam String soql) {
        try {
            JsonNode result = salesforceApiService.executeQuery(soql);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "soql", soql
            ));
        }
    }
    
    /**
     * 根据ID前缀推断对象类型
     */
    private String inferObjectTypeFromId(String id) {
        if (id == null || id.length() < 3) return "Account";
        
        String prefix = id.substring(0, 3);
        logger.info("Record ID prefix: {}", prefix);
        
        // 对象类型映射
        if ("001".equals(prefix)) return "Account";
        if ("003".equals(prefix)) return "Contact";
        if ("006".equals(prefix)) return "Opportunity";
        if ("500".equals(prefix)) return "Case";
        if ("00Q".equals(prefix)) return "Lead";
        if ("a6W".equals(prefix)) return "ffscpq_Estimate__c";
        
        return "Account";
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
            
            JsonNode result = salesforceApiService.createRecord("Task", fields);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "id", result.get("id").asText()
            ));
            
        } catch (Exception e) {
            logger.error("Error creating task", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PatchMapping("/account/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
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
