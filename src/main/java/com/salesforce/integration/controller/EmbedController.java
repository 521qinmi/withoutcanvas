package com.salesforce.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@Controller
public class EmbedController {
    private static final Logger logger = LoggerFactory.getLogger(EmbedController.class);
    
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
        
        // 确保 headers 正确设置
        response.setHeader("X-Frame-Options", "ALLOWALL");
        response.setHeader("Content-Security-Policy", "frame-ancestors *");
        
        // 添加表单数据模型（避免模板处理错误）
        Map<String, Object> formData = new HashMap<>();
        formData.put("sfRecordId", null);
        formData.put("sfObjectType", "Account");
        formData.put("sfObjectName", "Account");
        formData.put("accountName", "");
        formData.put("accountNumber", "");
        formData.put("phone", "");
        formData.put("address", "");
        
        model.addAttribute("recordId", recordId);
        model.addAttribute("appName", "Account Info");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("timestamp", System.currentTimeMillis());
        model.addAttribute("formData", formData);
        model.addAttribute("mode", "create"); // 默认创建模式
        
        return "form";
    }
    @GetMapping("/")
    public String home(HttpServletResponse response) {
        response.setHeader("X-Frame-Options", "ALLOWALL");
        return "redirect:/embed?recordId=001xx000003DGb2AAG";
    }
}
