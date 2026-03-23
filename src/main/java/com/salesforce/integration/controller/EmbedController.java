package com.salesforce.integration.controller;

import com.salesforce.integration.service.SalesforceApiService;
import com.salesforce.integration.service.SalesforceOAuthClient;
import com.salesforce.integration.service.FileStorageService;
import com.salesforce.integration.model.TokenInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@Controller
public class EmbedController {
    private static final Logger logger = LoggerFactory.getLogger(EmbedController.class);
    
    private final SalesforceApiService salesforceApiService;
    private final SalesforceOAuthClient oauthClient;
    private final FileStorageService fileStorageService;
    
    public EmbedController(SalesforceApiService salesforceApiService, 
                          SalesforceOAuthClient oauthClient,
                          FileStorageService fileStorageService) {
        this.salesforceApiService = salesforceApiService;
        this.oauthClient = oauthClient;
        this.fileStorageService = fileStorageService;
    }
    
    @GetMapping("/embed")
    public String embedPage(
            @RequestParam(defaultValue = "001xx000003DGb2AAG") String recordId,
            Model model,
            HttpServletResponse response) {
        
        logger.info("Embed page request - recordId: {}", recordId);
        
        // 确保 headers 正确设置
        response.setHeader("X-Frame-Options", "ALLOWALL");
        response.setHeader("Content-Security-Policy", "frame-ancestors *");
        
        model.addAttribute("recordId", recordId);
        model.addAttribute("appName", "Salesforce Java Integration");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("timestamp", System.currentTimeMillis());
        
        return "embed";
    }
    
    @GetMapping("/form")
    public String formPage(
            @RequestParam(defaultValue = "001xx000003DGb2AAG") String recordId,
            Model model,
            HttpServletResponse response) {
        
        logger.info("Form page request - recordId: {}", recordId);
        
        // 确保 headers 正确设置（用于 iframe 嵌入）
        response.setHeader("X-Frame-Options", "ALLOWALL");
        response.setHeader("Content-Security-Policy", "frame-ancestors *");
        
        try {
            // 1. 尝试从文件加载已保存的数据
            Map<String, Object> savedData = fileStorageService.loadAccountData(recordId);
            
            Map<String, Object> formData;
            String mode;
            
            if (savedData != null) {
                // 有已保存的数据，使用保存的数据
                logger.info("Found saved data for recordId: {}", recordId);
                formData = savedData;
                mode = "edit";
                
                // 确保 recordId 正确
                if (!formData.containsKey("sfRecordId") || formData.get("sfRecordId") == null) {
                    formData.put("sfRecordId", recordId);
                }
            } else {
                // 没有保存的数据，从 Salesforce 加载 Account 数据
                logger.info("No saved data found, loading from Salesforce for recordId: {}", recordId);
                formData = loadAccountFromSalesforce(recordId);
                formData.put("sfRecordId", recordId);
                mode = "create";
            }
            
            model.addAttribute("recordId", recordId);
            model.addAttribute("appName", "Account Form");
            model.addAttribute("version", "1.0.0");
            model.addAttribute("timestamp", System.currentTimeMillis());
            model.addAttribute("formData", formData);
            model.addAttribute("mode", mode);
            model.addAttribute("hasSavedData", savedData != null);
            
        } catch (Exception e) {
            logger.error("Error loading form data", e);
            
            // 如果出错，使用默认空数据
            Map<String, Object> formData = new HashMap<>();
            formData.put("sfRecordId", recordId);
            formData.put("accountName", "");
            formData.put("accountNumber", "");
            formData.put("phone", "");
            formData.put("address", "");
            
            model.addAttribute("recordId", recordId);
            model.addAttribute("formData", formData);
            model.addAttribute("mode", "create");
            model.addAttribute("hasSavedData", false);
            model.addAttribute("error", "Failed to load data: " + e.getMessage());
        }
        
        return "form";
    }
    
    /**
     * 从 Salesforce 加载 Account 数据
     */
    private Map<String, Object> loadAccountFromSalesforce(String recordId) throws Exception {
        Map<String, Object> formData = new HashMap<>();
        
        try {
            TokenInfo token = oauthClient.getAccessToken();
            JsonNode account = salesforceApiService.getAccountById(recordId);
            
            // 提取字段值
            if (account.has("Name")) {
                formData.put("accountName", account.get("Name").asText());
            }
            if (account.has("AccountNumber")) {
                formData.put("accountNumber", account.get("AccountNumber").asText());
            }
            if (account.has("Phone")) {
                formData.put("phone", account.get("Phone").asText());
            }
            if (account.has("BillingStreet")) {
                formData.put("address", account.get("BillingStreet").asText());
            }
            if (account.has("Industry")) {
                formData.put("industry", account.get("Industry").asText());
            }
            if (account.has("AnnualRevenue")) {
                formData.put("annualRevenue", account.get("AnnualRevenue").asText());
            }
            if (account.has("NumberOfEmployees")) {
                formData.put("numberOfEmployees", account.get("NumberOfEmployees").asText());
            }
            if (account.has("Description")) {
                formData.put("description", account.get("Description").asText());
            }
            if (account.has("Website")) {
                formData.put("website", account.get("Website").asText());
            }
            
            logger.info("Loaded account data from Salesforce: {}", formData.get("accountName"));
            
        } catch (Exception e) {
            logger.error("Failed to load account from Salesforce", e);
            // 返回空数据，让用户手动填写
        }
        
        return formData;
    }
    
    /**
     * 保存表单数据到文件
     */
    @PostMapping("/form/save")
    @ResponseBody
    public ResponseEntity<?> saveFormData(@RequestBody Map<String, Object> formData) {
        try {
            String recordId = (String) formData.get("sfRecordId");
            
            if (recordId == null || recordId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Record ID is required"
                ));
            }
            
            // 保存到文件
            boolean saved = fileStorageService.saveAccountData(recordId, formData);
            
            if (saved) {
                logger.info("Successfully saved form data for recordId: {}", recordId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Data saved successfully",
                    "recordId", recordId
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to save data"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error saving form data", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 检查是否有已保存的数据
     */
    @GetMapping("/form/check-saved")
    @ResponseBody
    public ResponseEntity<?> checkSavedData(@RequestParam String recordId) {
        try {
            boolean hasData = fileStorageService.hasSavedData(recordId);
            return ResponseEntity.ok(Map.of(
                "hasData", hasData,
                "recordId", recordId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/")
    public String home(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "ALLOWALL");
        return "redirect:/embed?recordId=001xx000003DGb2AAG";
    }
}
