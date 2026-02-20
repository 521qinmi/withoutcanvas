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
    
    @Value("${salesforce.oauth.username:}")
    private String username;
    
    @Value("${salesforce.oauth.password:}")
    private String password;
    
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
     * 获取访问令牌（支持客户端凭证流和密码模式）
     */
    public TokenInfo getAccessToken() throws Exception {
    TokenInfo cached = tokenCache.get("default");
    if (cached != null && !cached.isExpired()) {
        return cached;
    }
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", clientId);
    body.add("client_secret", clientSecret);
    body.add("username", username);
    body.add("password", password);
    // 明确指定 scope，匹配您在 External Client App 中设置的 scopes
    body.add("scope", "api refresh_token offline_access");
    
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
    
    try {
        logger.info("Requesting token with username: {}", username);
        logger.info("Token URL: {}", tokenUrl);
        
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // 记录令牌信息（隐藏敏感部分）
            logger.info("Token response received");
            logger.debug("Full response: {}", response.getBody());
            
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setAccessToken(json.get("access_token").asText());
            tokenInfo.setInstanceUrl(json.get("instance_url").asText());
            tokenInfo.setTokenType(json.get("token_type").asText());
            tokenInfo.setExpiresIn(json.get("expires_in").asInt());
            tokenInfo.setIssuedAt(System.currentTimeMillis());
            
            if (json.has("refresh_token")) {
                tokenInfo.setRefreshToken(json.get("refresh_token").asText());
            }
            
            tokenCache.put("default", tokenInfo);
            return tokenInfo;
        } else {
            throw new Exception("Failed to get token: " + response.getStatusCode());
        }
    } catch (Exception e) {
        logger.error("OAuth token error", e);
        
        // 如果是HTTP错误，提取详细响应
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
    
    private TokenInfo getMockToken() {
        TokenInfo mockToken = new TokenInfo();
        mockToken.setAccessToken("mock-access-token");
        mockToken.setInstanceUrl("https://bigdipper-pluto-4490.scratch.my.salesforce.com");
        mockToken.setTokenType("Bearer");
        mockToken.setExpiresIn(3600);
        mockToken.setIssuedAt(System.currentTimeMillis());
        return mockToken;
    }
}


