package com.salesforce.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.integration.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class SalesforceApiService {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceApiService.class);
    
    private final SalesforceOAuthClient oauthClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${salesforce.oauth.api-version:v59.0}")
    private String apiVersion;
    
    public SalesforceApiService(SalesforceOAuthClient oauthClient) {
        this.oauthClient = oauthClient;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 根据ID获取Account - 使用UriComponentsBuilder避免编码问题
     */
    public Map<String, Object> getAccountById(String accountId) throws Exception {
        logger.info("Getting account info for: {}", accountId);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        // 构建SOQL查询 - 使用最简单的格式
        String soql = "SELECT Id, Name, Phone, Website, Industry FROM Account WHERE Id = '" + accountId + "'";
        logger.info("SOQL: {}", soql);
        
        // 使用UriComponentsBuilder正确构建URL
        String url = UriComponentsBuilder.fromHttpUrl(tokenInfo.getInstanceUrl())
                .path("/services/data/" + apiVersion + "/query")
                .queryParam("q", soql)
                .build()
                .toUriString();
        
        logger.info("Request URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            logger.info("Response Status: {}", response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.has("records") && jsonResponse.get("records").size() > 0) {
                    JsonNode record = jsonResponse.get("records").get(0);
                    
                    Map<String, Object> account = new HashMap<>();
                    account.put("Id", getJsonProperty(record, "Id"));
                    account.put("Name", getJsonProperty(record, "Name"));
                    account.put("Phone", getJsonProperty(record, "Phone"));
                    account.put("Website", getJsonProperty(record, "Website"));
                    account.put("Industry", getJsonProperty(record, "Industry"));
                    
                    return account;
                } else {
                    throw new Exception("No records found for Account ID: " + accountId);
                }
            } else {
                throw new Exception("Query failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error executing query", e);
            throw e;
        }
    }
    
    /**
     * 测试查询 - 使用最简单的查询
     */
    public Map<String, Object> testSimpleQuery(String accountId) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        // 最简单的查询
        String soql = "SELECT Id, Name FROM Account WHERE Id = '" + accountId + "'";
        
        // 手动编码，但只编码一次
        String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/query?q=" + encodedSoql;
        
        logger.info("Test Query URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", response.getStatusCode().value());
        result.put("body", response.getBody());
        
        return result;
    }
    
    private String getJsonProperty(JsonNode node, String property) {
        if (node != null && node.has(property) && !node.get(property).isNull()) {
            return node.get(property).asText();
        }
        return null;
    }
}
