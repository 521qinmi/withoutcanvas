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
@RequestMapping("/api/auth-test")
public class AuthTestController {
    private static final Logger logger = LoggerFactory.getLogger(AuthTestController.class);
    
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
    
    @GetMapping("/direct")
    public Map<String, Object> testDirectAuth() {
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
        
        // 尝试不同的scope组合
        String[] scopeCombinations = {
            "api refresh_token",
            "api offline_access",
            "api refresh_token offline_access",
            "api",
            "full refresh_token"
        };
        
        Map<String, Object> attempts = new HashMap<>();
        
        for (String scope : scopeCombinations) {
            try {
                MultiValueMap<String, String> bodyWithScope = new LinkedMultiValueMap<>(body);
                bodyWithScope.add("scope", scope);
                
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(bodyWithScope, headers);
                
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
                
                attempts.put(scope, Map.of(
                    "success", true,
                    "status", response.getStatusCode().value(),
                    "has_access_token", response.getBody().containsKey("access_token"),
                    "has_refresh_token", response.getBody().containsKey("refresh_token"),
                    "instance_url", response.getBody().get("instance_url")
                ));
                
            } catch (Exception e) {
                attempts.put(scope, Map.of(
                    "success", false,
                    "error", e.getMessage()
                ));
                
                if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                    org.springframework.web.client.HttpClientErrorException httpEx = 
                        (org.springframework.web.client.HttpClientErrorException) e;
                    attempts.put(scope + "_details", Map.of(
                        "http_status", httpEx.getStatusCode().value(),
                        "response", httpEx.getResponseBodyAsString()
                    ));
                }
            }
        }
        
        result.put("attempts", attempts);
        return result;
    }
    
    @GetMapping("/simple-token")
    public Map<String, Object> getSimpleToken() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", password);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
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
}