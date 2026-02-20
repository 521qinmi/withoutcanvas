package com.salesforce.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EmbedController {
    private static final Logger logger = LoggerFactory.getLogger(EmbedController.class);
    
    @GetMapping("/embed")
    public String embedPage(
            @RequestParam String recordId,
            @RequestParam(required = false) String sfInstance,
            Model model) {
        
        model.addAttribute("recordId", recordId);
        model.addAttribute("sfInstance", sfInstance);
        model.addAttribute("appVersion", "1.0.0");
        
        return "embed";
    }
}