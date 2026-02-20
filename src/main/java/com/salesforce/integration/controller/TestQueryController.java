package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceOAuthClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-query")
public class TestQueryController {
    private static final Logger logger = LoggerFactory.getLogger(TestQueryController.class);
    
    @Autowired
    private SalesforceOAuthClient oauthClient;
    
    @GetMapping("/simple")
    public Map<String, Object> testSimpleQuery() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            var token = oauthClient.getAccessToken();
            result.put("token_obtained", true);
            result.put("instance_url", token.getInstanceUrl());
            
            // 测试最简单的查询 - 查询当前用户
            String soql = "SELECT Id, Name FROM User LIMIT 1";
            
            RestTemplate restTemplate = new RestTemplate();
            String url = token.getInstanceUrl() + "/services/data/v59.0/query?q=" + 
                        java.net.URLEncoder.encode(soql, "UTF-8");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            logger.info("Querying URL: {}", url);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            result.put("query_status", response.getStatusCode().value());
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(response.getBody());
            result.put("query_result", jsonResponse);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
            
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                result.put("http_status", httpEx.getStatusCode().value());
                result.put("response_body", httpEx.getResponseBodyAsString());
            }
        }
        
        return result;
    }
    
    @GetMapping("/account/{id}")
    public Map<String, Object> testAccountQuery(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("account_id", id);
        
        try {
            var token = oauthClient.getAccessToken();
            
            // 使用不同的API版本试试
            String[] versions = {"v58.0", "v59.0", "v60.0"};
            result.put("tested_versions", versions);
            
            for (String version : versions) {
                try {
                    String soql = "SELECT Id, Name FROM Account WHERE Id = '" + id + "'";
                    String url = token.getInstanceUrl() + "/services/data/" + version + "/query?q=" + 
                                java.net.URLEncoder.encode(soql, "UTF-8");
                    
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token.getAccessToken());
                    
                    HttpEntity<String> request = new HttpEntity<>(headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                    
                    result.put("version_" + version + "_status", response.getStatusCode().value());
                    
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonResponse = mapper.readTree(response.getBody());
                    result.put("version_" + version + "_result", jsonResponse);
                    
                } catch (Exception e) {
                    result.put("version_" + version + "_error", e.getMessage());
                }
            }
            
        } catch (Exception e) {
            result.put("auth_error", e.getMessage());
        }
        
        return result;
    }
}