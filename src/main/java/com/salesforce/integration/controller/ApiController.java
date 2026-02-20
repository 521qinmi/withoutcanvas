package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceService;

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
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        logger.info("Test API called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Java API is working");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", "railway");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /*
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        logger.info("Get account: {}", id);
        
        Map<String, Object> account = new HashMap<>();
        account.put("Id", id);
        account.put("Name", "测试公司");
        account.put("Phone", "010-12345678");
        account.put("Website", "www.example.com");
        account.put("Industry", "Technology");
        account.put("Description", "从Java应用返回的数据");
        
        return ResponseEntity.ok(account);
    }
    */
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        try {
            logger.info("Getting account from Salesforce: {}", id);
            Map<String, Object> account = salesforceService.getAccountById(id);
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
    @PostMapping("/message")
    public ResponseEntity<?> receiveMessage(@RequestBody Map<String, Object> message) {
        logger.info("Received message: {}", message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "received");
        response.put("messageId", System.currentTimeMillis());
        response.put("original", message);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/debug")
    public ResponseEntity<?> debug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("timestamp", System.currentTimeMillis());
        debug.put("javaVersion", System.getProperty("java.version"));
        debug.put("os", System.getProperty("os.name"));
        debug.put("availableEndpoints", new String[]{"/api/test", "/api/health", "/api/account/{id}", "/api/message", "/api/debug"});
        
        return ResponseEntity.ok(debug);
    }
}


