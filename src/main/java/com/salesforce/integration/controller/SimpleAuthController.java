package com.salesforce.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-auth")
public class SimpleAuthController {
    private static final Logger logger = LoggerFactory.getLogger(SimpleAuthController.class);
    
    @Value("${salesforce.oauth.client-id:}")
    private String clientId;
    
    @Value("${salesforce.oauth.client-secret:}")
    private String clientSecret;
    
    @Value("${salesforce.oauth.username:}")
    private String username;
    
    @Value("${salesforce.oauth.password:}")
    private String password;
    
    @Value("${salesforce.oauth.token-url:}")
    private String tokenUrl;
    
    @GetMapping("/test")
    public Map<String, Object> testAuth() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("token_url", tokenUrl);
        result.put("username", username);
        
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);
        
        // 注意：不添加 scope 参数
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            logger.info("Attempting authentication without scope parameter");
            
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            result.put("success", true);
            result.put("status", response.getStatusCode().value());
            result.put("response", response.getBody());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                result.put("http_status", httpEx.getStatusCode().value());
                result.put("response_body", httpEx.getResponseBodyAsString());
            }
        }
        
        return result;
    }
    
    @GetMapping("/check-config")
    public Map<String, Object> checkConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("client_id", maskString(clientId));
        config.put("client_secret", maskString(clientSecret));
        config.put("username", username);
        config.put("password", password != null ? "****" : null);
        config.put("token_url", tokenUrl);
        config.put("all_configured", 
            clientId != null && !clientId.isEmpty() &&
            clientSecret != null && !clientSecret.isEmpty() &&
            username != null && !username.isEmpty() &&
            password != null && !password.isEmpty() &&
            tokenUrl != null && !tokenUrl.isEmpty());
        return config;
    }
    
    private String maskString(String input) {
        if (input == null || input.length() < 8) return "****";
        return input.substring(0, 4) + "..." + input.substring(input.length() - 4);
    }
}