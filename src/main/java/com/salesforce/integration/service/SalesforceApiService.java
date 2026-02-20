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
 * 获取Estimate记录 - 确保对象名正确
 */
public Map<String, Object> getEstimateById(String estimateId) throws Exception {
    logger.info("Getting estimate info for: {}", estimateId);
    
    TokenInfo tokenInfo = oauthClient.getAccessToken();
    
    // 关键修正：对象名必须是 ffscpq_Estimate__c (两个下划线)
    String objectName = "ffscpq_Estimate__c";
    String soql = "SELECT Id, Name FROM " + objectName + " WHERE Id = '" + estimateId + "'";
    
    logger.info("SOQL: {}", soql);
    logger.info("Object Name: {}", objectName);
    
    JsonNode result = executeQuery(soql);
    
    if (result != null && result.has("records") && result.get("records").size() > 0) {
        JsonNode record = result.get("records").get(0);
        
        Map<String, Object> estimate = new HashMap<>();
        estimate.put("Id", getJsonProperty(record, "Id"));
        estimate.put("Name", getJsonProperty(record, "Name"));
        estimate.put("ObjectType", objectName);
        
        return estimate;
    } else {
        throw new Exception("Estimate not found: " + estimateId + " in object " + objectName);
    }
} 
    /**
 * 通用记录查询 - 适用于任何对象
 */
public Map<String, Object> getRecordById(String objectType, String recordId) throws Exception {
    logger.info("Getting {} record: {}", objectType, recordId);
    
    TokenInfo tokenInfo = oauthClient.getAccessToken();
    
    // 不使用 FIELDS(ALL)，而是查询特定字段
    // 先获取对象的字段列表，或者使用常用的字段
    String soql;
    
    if (objectType.equals("Account")) {
        soql = "SELECT Id, Name, Phone, Website, Industry, Type, Description, AnnualRevenue " +
               "FROM Account WHERE Id = '" + recordId + "'";
    } else if (objectType.equals("ffscpq_Estimate__c")) {
        // 为 Estimate 对象查询常用字段
        soql = "SELECT Id, Name, ffscpq_Amount__c, ffscpq_Status__c " +
               "FROM ffscpq_Estimate__c WHERE Id = '" + recordId + "'";
    } else {
        // 通用回退 - 只查询Id和Name
        soql = "SELECT Id, Name FROM " + objectType + " WHERE Id = '" + recordId + "'";
    }
    
    logger.info("SOQL: {}", soql);
    
    JsonNode result = executeQuery(soql);
    
    if (result != null && result.has("records") && result.get("records").size() > 0) {
        JsonNode record = result.get("records").get(0);
        
        // 转换为Map
        Map<String, Object> resultMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
        
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            
            if (!fieldValue.isNull() && !fieldValue.isObject() && !fieldValue.isArray()) {
                if (fieldValue.isTextual()) {
                    resultMap.put(fieldName, fieldValue.asText());
                } else if (fieldValue.isNumber()) {
                    resultMap.put(fieldName, fieldValue.asDouble());
                } else if (fieldValue.isBoolean()) {
                    resultMap.put(fieldName, fieldValue.asBoolean());
                } else {
                    resultMap.put(fieldName, fieldValue.toString());
                }
            }
        }
        
        return resultMap;
    } else {
        throw new Exception("Record not found: " + recordId);
    }
}
    
    public JsonNode executeQuery(String soql) throws Exception {
    TokenInfo tokenInfo = oauthClient.getAccessToken();
    
    String encodedSoql = URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
    String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + 
                "/query?q=" + encodedSoql;
    
    logger.info("Executing query: {}", soql);
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






