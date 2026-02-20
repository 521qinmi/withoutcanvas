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
     * 执行SOQL查询
     */
    public JsonNode executeQuery(String soql) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + 
                    "/query?q=" + java.net.URLEncoder.encode(soql, "UTF-8");
        
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
     * 根据ID获取Account
     */
    public Map<String, Object> getAccountById(String accountId) throws Exception {
        logger.info("Getting account info for: {}", accountId);
        
        String soql = String.format(
            "SELECT Id, Name, Phone, Website, Industry, Type, Description, " +
            "AnnualRevenue, BillingStreet, BillingCity, BillingState, BillingPostalCode, " +
            "BillingCountry, Owner.Name, CreatedDate, LastModifiedDate " +
            "FROM Account WHERE Id = '%s'", 
            accountId
        );
        
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
            account.put("AnnualRevenue", getJsonProperty(record, "AnnualRevenue"));
            
            return account;
        } else {
            throw new Exception("Account not found: " + accountId);
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
     * 删除记录
     */
    public void deleteRecord(String objectType, String recordId) throws Exception {
        TokenInfo tokenInfo = oauthClient.getAccessToken();
        
        String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/" + objectType + "/" + recordId;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenInfo.getAccessToken());
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new Exception("Delete failed: " + response.getStatusCode());
        }
    }
    
    /**
     * 获取JSON属性，处理null值
     */
    private String getJsonProperty(JsonNode node, String property) {
        if (node.has(property) && !node.get(property).isNull()) {
            return node.get(property).asText();
        }
        return null;
    }
}
