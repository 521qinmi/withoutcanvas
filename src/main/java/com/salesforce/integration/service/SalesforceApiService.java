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
            throw new Exception("Query failed: " + response.getStatusCode());
        }
    }
    
    public JsonNode getAccountById(String accountId) throws Exception {
        String soql = String.format(
            "SELECT Id, Name, Phone, Website, Industry, AnnualRevenue, Description, " +
            "Owner.Name FROM Account WHERE Id = '%s'", accountId);
        
        JsonNode result = executeQuery(soql);
        JsonNode records = result.get("records");
        
        if (records != null && records.size() > 0) {
            return records.get(0);
        } else {
            throw new Exception("Account not found: " + accountId);
        }
    }
}