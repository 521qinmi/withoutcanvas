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
 * 执行SOQL查询
 */
public JsonNode executeQuery(String soql) throws Exception {
    TokenInfo tokenInfo = oauthClient.getAccessToken();
    
    // 重要：只编码一次！
    String encodedSoql = java.net.URLEncoder.encode(soql, StandardCharsets.UTF_8.toString());
    String url = tokenInfo.getInstanceUrl() + "/services/data/" + apiVersion + 
                "/query?q=" + encodedSoql;
    
    logger.info("Original SOQL: {}", soql);
    logger.info("Encoded SOQL: {}", encodedSoql);
    logger.info("Full URL: {}", url);
    
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
 * 根据ID获取Account
 */
public Map<String, Object> getAccountById(String accountId) throws Exception {
    logger.info("Getting account info for: {}", accountId);
    
    // 构建干净的SOQL查询
    String soql = "SELECT Id, Name, Phone, Website, Industry, Type, Description, AnnualRevenue " +
                  "FROM Account WHERE Id = '" + accountId + "'";
    
    logger.info("SOQL Query: {}", soql);
    
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
     * 获取JSON属性，处理null值
     */
    private String getJsonProperty(JsonNode node, String property) {
        if (node != null && node.has(property) && !node.get(property).isNull()) {
            return node.get(property).asText();
        }
        return null;
    }
}


