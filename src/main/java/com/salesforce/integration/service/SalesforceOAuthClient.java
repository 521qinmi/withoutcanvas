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
    
    public TokenInfo getAccessToken() throws Exception {
        // 检查缓存
        String cacheKey = "default";
        TokenInfo cached = tokenCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            logger.debug("Using cached access token");
            return cached;
        }
        
        // 如果没有配置凭证，返回模拟令牌
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            logger.warn("Salesforce credentials not configured, using mock token");
            return getMockToken();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        
        // 优先使用用户名密码模式
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", password);
            // 明确指定scope
            body.add("scope", "api refresh_token offline_access");
            logger.info("Using password grant flow with username: {}", username);
        } else {
            // 客户端凭证流
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            logger.info("Using client credentials flow");
        }
        
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
                
                // 如果有refresh_token，保存它
                if (json.has("refresh_token")) {
                    tokenInfo.setRefreshToken(json.get("refresh_token").asText());
                }
                
                // 如果有其他字段，也保存
                if (json.has("id")) {
                    tokenInfo.setId(json.get("id").asText());
                }
                if (json.has("signature")) {
                    tokenInfo.setSignature(json.get("signature").asText());
                }
                if (json.has("scope")) {
                    tokenInfo.setScope(json.get("scope").asText());
                }
                
                tokenCache.put(cacheKey, tokenInfo);
                logger.info("Successfully obtained access token, expires in: {} seconds", tokenInfo.getExpiresIn());
                
                // 验证令牌是否立即可用
                validateToken(tokenInfo);
                
                return tokenInfo;
            } else {
                throw new Exception("Failed to get token: " + response.getStatusCode() + " - " + response.getBody());
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
    
    private void validateToken(TokenInfo token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = token.getInstanceUrl() + "/services/data/v59.0/sobjects/";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            logger.info("Token validation successful, status: {}", response.getStatusCode());
            
        } catch (Exception e) {
            logger.warn("Token validation failed, but token might still work: {}", e.getMessage());
            // 不抛出异常，因为有些Org可能不允许这个端点
        }
    }
    
    public void clearTokenCache() {
        tokenCache.clear();
        logger.info("Token cache cleared");
    }
    
    private TokenInfo getMockToken() {
        TokenInfo mockToken = new TokenInfo();
        mockToken.setAccessToken("mock-access-token");
        mockToken.setInstanceUrl("https://yourdomain.my.salesforce.com");
        mockToken.setTokenType("Bearer");
        mockToken.setExpiresIn(3600);
        mockToken.setIssuedAt(System.currentTimeMillis());
        return mockToken;
    }
}
