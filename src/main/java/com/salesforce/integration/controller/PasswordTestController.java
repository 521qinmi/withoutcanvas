package com.salesforce.integration.controller;

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
@RequestMapping("/api/password-test")
public class PasswordTestController {
    
    @Value("${salesforce.oauth.client-id:}")
    private String clientId;
    
    @Value("${salesforce.oauth.client-secret:}")
    private String clientSecret;
    
    @Value("${salesforce.oauth.username:}")
    private String username;
    
    @Value("${salesforce.oauth.token-url:}")
    private String tokenUrl;
    
    @GetMapping("/test")
    public Map<String, Object> testPassword(@Value("${salesforce.oauth.password:}") String password) {
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("token_url", tokenUrl);
        result.put("password_length", password != null ? password.length() : 0);
        
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
        
        try {
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
    
    @GetMapping("/format-help")
    public Map<String, Object> formatHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("message", "您的密码格式应该是: 您的Salesforce密码 + 安全令牌");
        help.put("example", "如果密码是 'mypassword123'，安全令牌是 'XYZ789ABC'，则密码应为 'mypassword123XYZ789ABC'");
        help.put("note", "有些Scratch Org可能不需要安全令牌，可以只试密码");
        help.put("current_username", username);
        return help;
    }
}