package com.salesforce.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.integration.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SalesforceOAuthClient {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceOAuthClient.class);
    
    @Value("${salesforce.oauth.client-id}")
    private String clientId;
    
    @Value("${salesforce.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${salesforce.oauth.token-url}")
    private String tokenUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    public SalesforceOAuthClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public TokenInfo getAccessToken() throws Exception {
        TokenInfo cached = tokenCache.get("default");
        if (cached != null && !cached.isExpired()) {
            return cached;
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                
                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.setAccessToken(json.get("access_token").asText());
                tokenInfo.setInstanceUrl(json.get("instance_url").asText());
                tokenInfo.setExpiresIn(json.get("expires_in").asInt());
                tokenInfo.setIssuedAt(System.currentTimeMillis());
                
                tokenCache.put("default", tokenInfo);
                return tokenInfo;
            } else {
                throw new Exception("Failed to get token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("OAuth token error", e);
            throw e;
        }
    }
}