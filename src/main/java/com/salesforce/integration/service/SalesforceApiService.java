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
     * 根据ID获取Account
     */
    public Map<String, Object> getAccountById(String accountId) throws Exception {
        logger.info("Getting account info for: {}", accountId);
        return getRecordById("Account", accountId);
    }
    
    /**
     * 根据ID获取Estimate
     */
    public Map<String, Object> getEstimateById(String estimateId) throws Exception {
        logger.info("Getting estimate info for: {}", estimateId);
        return getRecordById("ffscpq_Estimate__c", estimateId);
    }
    
    /**
     * 通用记录查询 - 适用于任何对象
     */
    public Map<String, Object> getRecordById(String objectType, String recordId) throws Exception {
        logger.info("Getting {} record: {}", objectType, recordId);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        // 使用SOQL查询
        String soql = String.format("SELECT FIELDS(ALL) FROM %s WHERE Id = '%s' LIMIT 1", 
                                    objectType, recordId);
        
        String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + 
                    "/query?q=" + encodedSoql;
        
        logger.info("Query URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            if (jsonResponse.has("records") && jsonResponse.get("records").size() > 0) {
                JsonNode record = jsonResponse.get("records").get(0);
                
                // 转换为Map
                Map<String, Object> result = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
                
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String fieldName = field.getKey();
                    JsonNode fieldValue = field.getValue();
                    
                    if (!fieldValue.isNull() && !fieldValue.isObject()) {
                        if (fieldValue.isTextual()) {
                            result.put(fieldName, fieldValue.asText());
                        } else if (fieldValue.isNumber()) {
                            result.put(fieldName, fieldValue.asDouble());
                        } else if (fieldValue.isBoolean()) {
                            result.put(fieldName, fieldValue.asBoolean());
                        } else if (fieldValue.isArray()) {
                            result.put(fieldName, fieldValue.toString());
                        } else {
                            result.put(fieldName, fieldValue.toString());
                        }
                    }
                }
                
                return result;
            } else {
                throw new Exception("Record not found: " + recordId);
            }
        } else {
            throw new Exception("Query failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
    
    /**
     * 执行SOQL查询
     */
    public JsonNode executeQuery(String soql) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + 
                    "/query?q=" + encodedSoql;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            return objectMapper.readTree(response.getBody());
        } else {
            throw new Exception("Query failed: " + response.getStatusCode() + " - " + response.getBody());
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
     * 更新记录
     */
    public Map<String, Object> updateRecord(String objectType, String recordId, Map<String, Object> updates) throws Exception {
        logger.info("Updating {} record {} with: {}", objectType, recordId, updates);
        
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/" + objectType + "/" + recordId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String jsonBody = objectMapper.writeValueAsString(updates);
        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            return getRecordById(objectType, recordId);
        } else {
            throw new Exception("Update failed: " + response.getStatusCode() + " - " + response.getBody());
        }
    }
    
    /**
     * 获取JSON属性
     */
    private String getJsonProperty(JsonNode node, String property) {
        if (node != null && node.has(property) && !node.get(property).isNull()) {
            return node.get(property).asText();
        }
        return null;
    }
}

