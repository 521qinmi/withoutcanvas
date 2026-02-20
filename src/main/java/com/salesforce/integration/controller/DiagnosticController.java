package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/diag")
public class DiagnosticController {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticController.class);
    
    @Autowired(required = false)
    private SalesforceApiService salesforceApiService;
    
    @Autowired(required = false)
    private SalesforceOAuthClient oauthClient;
    
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "DiagnosticController");
        status.put("timestamp", System.currentTimeMillis());
        status.put("salesforceApiService", salesforceApiService != null ? "available" : "not available");
        status.put("oauthClient", oauthClient != null ? "available" : "not available");
        return status;
    }
    
    @GetMapping("/test-account/{id}")
    public Map<String, Object> testAccount(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", id);
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            if (salesforceApiService == null) {
                result.put("error", "SalesforceApiService not available");
                return result;
            }
            
            // 尝试获取token
            result.put("attempting", "Getting access token");
            var token = oauthClient.getAccessToken();
            result.put("tokenObtained", true);
            result.put("instanceUrl", token.getInstanceUrl());
            
            // 尝试查询account
            result.put("attempting", "Querying account");
            Map<String, Object> account = salesforceApiService.getAccountById(id);
            result.put("accountFound", true);
            result.put("account", account);
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            
            logger.error("Diagnostic error for account {}: {}", id, e.getMessage(), e);
        }
        
        return result;
    }
    
    @GetMapping("/simple-account/{id}")
    public ResponseEntity<?> simpleAccount(@PathVariable String id) {
        try {
            // 直接返回模拟数据，测试前端是否正常
            Map<String, Object> account = new HashMap<>();
            account.put("Id", id);
            account.put("Name", "诊断测试公司");
            account.put("Phone", "010-12345678");
            account.put("Website", "www.diagnostic.com");
            account.put("Industry", "Diagnostic");
            account.put("Description", "这是诊断测试返回的数据");
            account.put("source", "diagnostic-mock");
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}