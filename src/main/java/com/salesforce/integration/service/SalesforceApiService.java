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
     * 根据ID获取Account
     */
    public Map<String, Object> getAccountById(String accountId) throws Exception {
        logger.info("Getting account info for: {}", accountId);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String soql = "SELECT Id, Name, Phone, Website, Industry, Type, Description, AnnualRevenue " +
                      "FROM Account WHERE Id = '" + accountId + "'";
        
        String url = UriComponentsBuilder.fromHttpUrl(tokenInfo.getInstanceUrl())
                .path("/services/data/" + apiVersion + "/query")
                .queryParam("q", soql)
                .build()
                .toUriString();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        
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
                account.put("Type", getJsonProperty(record, "Type"));
                account.put("Description", getJsonProperty(record, "Description"));
                
                if (record.has("AnnualRevenue") && !record.get("AnnualRevenue").isNull()) {
                    account.put("AnnualRevenue", record.get("AnnualRevenue").asDouble());
                }
                
                return account;
            } else {
                throw new Exception("Account not found: " + accountId);
            }
        } else {
            throw new Exception("Query failed: " + response.getStatusCode());
        }
    }
    
    /**
     * 创建记录（Task, Account等）
     */
    public JsonNode createRecord(String objectType, Map<String, Object> fields) throws Exception {
        logger.info("Creating {} record with fields: {}", objectType, fields);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/" + objectType;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jsonBody = objectMapper.writeValueAsString(fields);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readTree(response.getBody());
        } else {
            throw new Exception("Create failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
    
    /**
     * 更新Account
     */
    public Map<String, Object> updateAccount(String accountId, Map<String, Object> updates) throws Exception {
        logger.info("Updating account {} with: {}", accountId, updates);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/Account/" + accountId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jsonBody = objectMapper.writeValueAsString(updates);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            // 返回更新后的Account
            return getAccountById(accountId);
        } else {
            throw new Exception("Update failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
    
    /**
     * 测试简单查询
     */
    public Map<String, Object> testSimpleQuery(String accountId) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String soql = "SELECT Id, Name FROM Account WHERE Id = '" + accountId + "'";
        String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/query?q=" + encodedSoql;
        
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
