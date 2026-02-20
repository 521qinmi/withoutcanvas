package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.model.TokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    
    /**
     * 通用记录查询接口 - 根据ID前缀自动识别对象类型
     */
    @GetMapping("/record/{id}")
    public ResponseEntity<?> getRecord(@PathVariable String id) {
        try {
            logger.info("Getting record from Salesforce: {}", id);
            
            // 根据ID前缀确定对象类型
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
    
    /**
     * Estimate专用接口
     */
    @GetMapping("/estimate/{id}")
    public ResponseEntity<?> getEstimate(@PathVariable String id) {
        try {
            logger.info("Getting estimate from Salesforce: {}", id);
            Map<String, Object> estimate = salesforceApiService.getEstimateById(id);
            return ResponseEntity.ok(estimate);
        } catch (Exception e) {
            logger.error("Error getting estimate: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("estimateId", id);
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
        Map<String, String> prefixMap = new HashMap<>();
        prefixMap.put("001", "Account");
        prefixMap.put("003", "Contact");
        prefixMap.put("006", "Opportunity");
        prefixMap.put("500", "Case");
        prefixMap.put("00Q", "Lead");
        prefixMap.put("a6W", "Estimate__c");  // 您的Estimate对象
        prefixMap.put("a0X", "CustomObject__c");
        
        return prefixMap.getOrDefault(prefix, "Account");
    }
    
    // ... 其他方法保持不变 (createTask, updateAccount, refreshToken, health)
}
