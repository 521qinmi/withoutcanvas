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
import java.util.Iterator;
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
     * 执行SOQL查询 - 使用 UriComponentsBuilder 避免编码问题
     */
    public JsonNode executeQuery(String soql) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        // 使用 UriComponentsBuilder 构建 URL，它会自动处理编码
        String url = UriComponentsBuilder.fromHttpUrl(tokenInfo.getInstanceUrl())
                .path("/services/data/" + apiVersion + "/query")
                .queryParam("q", soql)
                .build()
                .toUriString();
        
        logger.info("Original SOQL: {}", soql);
        logger.info("Encoded URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readTree(response.getBody());
            } else {
                throw new Exception("Query failed: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            logger.error("Query execution failed", e);
            throw e;
        }
    }
    
    /**
     * 获取Account记录
     */
    public Map<String, Object> getAccountById(String accountId) throws Exception {
        logger.info("Getting account info for: {}", accountId);
        
        String soql = "SELECT Id, Name, Phone, Website, Industry, Type, Description, AnnualRevenue " +
                     "FROM Account WHERE Id = '" + accountId + "'";
        
        JsonNode result = executeQuery(soql);
        
        if (result != null && result.has("records") && result.get("records").size() > 0) {
            JsonNode record = result.get("records").get(0);
            
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
    }
    
    /**
     * 获取Estimate记录
     */
    public Map<String, Object> getEstimateById(String estimateId) throws Exception {
        logger.info("Getting estimate info for: {}", estimateId);
        
        String soql = "SELECT Id, Name FROM ffscpq__Estimate__c WHERE Id = '" + estimateId + "'";
        
        JsonNode result = executeQuery(soql);
        
        if (result != null && result.has("records") && result.get("records").size() > 0) {
            JsonNode record = result.get("records").get(0);
            
            Map<String, Object> estimate = new HashMap<>();
            estimate.put("Id", getJsonProperty(record, "Id"));
            estimate.put("Name", getJsonProperty(record, "Name"));
            
            return estimate;
        } else {
            throw new Exception("Estimate not found: " + estimateId);
        }
    }
    
    /**
     * 通用记录查询
     */
    public Map<String, Object> getRecordById(String objectType, String recordId) throws Exception {
        logger.info("Getting {} record: {}", objectType, recordId);
        
        String soql = "SELECT Id, Name FROM " + objectType + " WHERE Id = '" + recordId + "'";
        
        JsonNode result = executeQuery(soql);
        
        if (result != null && result.has("records") && result.get("records").size() > 0) {
            JsonNode record = result.get("records").get(0);
            
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("Id", getJsonProperty(record, "Id"));
            resultMap.put("Name", getJsonProperty(record, "Name"));
            resultMap.put("ObjectType", objectType);
            
            return resultMap;
        } else {
            throw new Exception("Record not found: " + recordId + " in object " + objectType);
        }
    }
    
    /**
     * 创建记录
     */
    public JsonNode createRecord(String objectType, Map<String, Object> fields) throws Exception {
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
     * 更新Account记录
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
     * 获取JSON属性
     */
    private String getJsonProperty(JsonNode node, String property) {
        if (node != null && node.has(property) && !node.get(property).isNull()) {
            JsonNode value = node.get(property);
            if (value.isTextual()) {
                return value.asText();
            } else {
                return value.toString();
            }
        }
        return null;
    }
}

