package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.model.TokenInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    private final SalesforceApiService salesforceApiService;
    private final SalesforceOAuthClient oauthClient;
    
    public ApiController(SalesforceApiService salesforceApiService, SalesforceOAuthClient oauthClient) {
        this.salesforceApiService = salesforceApiService;
        this.oauthClient = oauthClient;
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Java API is working");
        response.put("timestamp", System.currentTimeMillis());
        response.put("server", "railway");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/record/{id}")
    public ResponseEntity<?> getRecord(@PathVariable String id) {
        try {
            logger.info("Getting record: {}", id);
            
            // 根据ID前缀推断对象类型
            String objectType = inferObjectTypeFromId(id);
            logger.info("Inferred object type: {}", objectType);
            
            Map<String, Object> record = salesforceApiService.getRecordById(objectType, id);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.error("Error getting record: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("recordId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
    * 根据ID前缀推断对象类型
    */
    private String inferObjectTypeFromId(String id) {
        if (id == null || id.length() < 3) return "Account";
        
        String prefix = id.substring(0, 3);
        logger.info("Record ID prefix: {}", prefix);
        
        // 对象类型映射
        if ("001".equals(prefix)) return "Account";
        if ("003".equals(prefix)) return "Contact";
        if ("006".equals(prefix)) return "Opportunity";
        if ("500".equals(prefix)) return "Case";
        if ("00Q".equals(prefix)) return "Lead";
        if ("a6W".equals(prefix)) return "ffscpq_Estimate__c";  // 两个下划线
        
        return "Account";
    }
    
    // 删除原来的 getEstimate 方法，或者保留但调用通用方法
    // @GetMapping("/estimate/{id}")
    // public ResponseEntity<?> getEstimate(@PathVariable String id) {
    //     try {
    //         logger.info("Getting estimate via estimate endpoint: {}", id);
    //         // 直接调用通用方法，但指定对象类型
    //         Map<String, Object> estimate = salesforceApiService.getEstimateById("ffscpq_Estimate__c", id);
    //         return ResponseEntity.ok(estimate);
    //     } catch (Exception e) {
    //         logger.error("Error getting estimate: {}", e.getMessage(), e);
    //         return ResponseEntity.status(500).body(Map.of(
    //             "error", e.getMessage(),
    //             "estimateId", id,
    //             "objectType", "ffscpq_Estimate__c"
    //         ));
    //     }
    // }
    @GetMapping("/estimate/{id}")
    public ResponseEntity<?> getEstimate(@PathVariable String id) {
        try {
            logger.info("Getting estimate: {}", id);
            
            TokenInfo token = oauthClient.getAccessToken();
            
            // 先查询对象是否存在
            String describeUrl = token.getInstanceUrl() + "/services/data/" + apiVersion + "/sobjects/";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.getAccessToken());
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            
            // 获取所有可用的对象列表
            ResponseEntity<String> describeResponse = restTemplate.exchange(describeUrl, HttpMethod.GET, request, String.class);
            
            if (describeResponse.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode sobjects = mapper.readTree(describeResponse.getBody());
                
                List<String> objectNames = new ArrayList<>();
                for (JsonNode obj : sobjects) {
                    objectNames.add(obj.get("name").asText());
                }
                
                // 查找包含 "Estimate" 的对象
                List<String> estimateObjects = objectNames.stream()
                    .filter(name -> name.contains("Estimate"))
                    .collect(Collectors.toList());
                
                return ResponseEntity.ok(Map.of(
                    "allObjects", objectNames,
                    "estimateObjects", estimateObjects
                ));
            }
            
            return ResponseEntity.ok(Map.of("error", "Could not retrieve objects"));
            
        } catch (Exception e) {
            logger.error("Error: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    /**
    * 保留原有Account接口向后兼容
    */
    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        try {
            logger.info("Getting account from Salesforce: {}", id);
            Map<String, Object> account = salesforceApiService.getAccountById(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            logger.error("Error getting account: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("accountId", id);
            error.put("status", "failed");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/debug/check-object/{id}")
    public ResponseEntity<?> checkObjectName(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", id);
        
        try {
            TokenInfo token = oauthClient.getAccessToken();
            
            // 尝试不同的对象名变体
            String[] possibleNames = {
                "ffscpq_Estimate__c",
                    "ffscpq_Estimate_c",
                    "Estimate__c",
                    "ffscpq_Estimate"
                    };
                        
                        Map<String, Object> attempts = new HashMap<>();
            
            for (String objectName : possibleNames) {
                try {
                    String soql = "SELECT Id FROM " + objectName + " WHERE Id = '" + id + "' LIMIT 1";
                    
                    String url = UriComponentsBuilder.fromHttpUrl(token.getInstanceUrl())
                        .path("/services/data/" + apiVersion + "/query")
                        .queryParam("q", soql)
                        .build()
                        .toUriString();
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(token.getAccessToken());
                    
                    HttpEntity<String> request = new HttpEntity<>(headers);
                    RestTemplate restTemplate = new RestTemplate();
                    
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
                    
                    attempts.put(objectName, Map.of(
                        "success", true,
                        "status", response.getStatusCode().value(),
                        "response", response.getBody()
                    ));
                    
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                        org.springframework.web.client.HttpClientErrorException httpEx = 
                            (org.springframework.web.client.HttpClientErrorException) e;
                        errorMsg = httpEx.getResponseBodyAsString();
                    }
                    
                    attempts.put(objectName, Map.of(
                        "success", false,
                        "error", errorMsg
                    ));
                }
            }
            
            result.put("attempts", attempts);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    // ... 其他方法保持不变 (createTask, updateAccount, refreshToken, health)
}

