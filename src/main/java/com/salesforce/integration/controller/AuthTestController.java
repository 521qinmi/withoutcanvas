package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthTestController {
    private static final Logger logger = LoggerFactory.getLogger(AuthTestController.class);
    
    @Autowired
    private SalesforceOAuthClient oauthClient;
    
    @GetMapping("/test")
    public Map<String, Object> testAuth() {
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
        
        return result;
    }
    
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "AuthTestController");
        status.put("status", "running");
        status.put("timestamp", System.currentTimeMillis());
        status.put("auth_type", "Client Credentials Flow");
        return status;
    }
}
