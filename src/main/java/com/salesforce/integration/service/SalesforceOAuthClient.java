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
    
    @Value("${salesforce.oauth.client-id:}")
    private String clientId;
    
    @Value("${salesforce.oauth.client-secret:}")
    private String clientSecret;
    
    @Value("${salesforce.oauth.token-url:}")
    private String tokenUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    public SalesforceOAuthClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取访问令牌（客户端凭证流）
     */
    public TokenInfo getAccessToken() throws Exception {
        // 检查缓存
        String cacheKey = "default";
        TokenInfo cached = tokenCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Using cached access token");
            return cached;
        }
        
        // 检查凭证是否配置
        if (clientId == null || clientId.isEmpty() || 
            clientSecret == null || clientSecret.isEmpty() ||
            tokenUrl == null || tokenUrl.isEmpty()) {
            throw new Exception("Salesforce OAuth credentials not configured. Please check environment variables.");
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            logger.info("Requesting access token from: {}", tokenUrl);
            
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                
                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.setAccessToken(json.get("access_token").asText());
                tokenInfo.setInstanceUrl(json.get("instance_url").asText());
                tokenInfo.setTokenType(json.get("token_type").asText());
                tokenInfo.setExpiresIn(json.get("expires_in").asInt());
                tokenInfo.setIssuedAt(System.currentTimeMillis());
                
                tokenCache.put(cacheKey, tokenInfo);
                logger.info("Successfully obtained access token, expires in: {} seconds", tokenInfo.getExpiresIn());
                
                return tokenInfo;
            } else {
                throw new Exception("Failed to get token: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            logger.error("OAuth token error", e);
            
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                logger.error("HTTP Status: {}", httpEx.getStatusCode());
                logger.error("Response Body: {}", httpEx.getResponseBodyAsString());
            }
            
            throw e;
        }
    }
    
    /**
     * 清除令牌缓存
     */
    public void clearTokenCache() {
        tokenCache.clear();
        logger.info("Token cache cleared");
    }
}
