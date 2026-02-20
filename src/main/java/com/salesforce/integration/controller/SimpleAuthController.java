package com.salesforce.integration.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-auth")
public class SimpleAuthController {
    
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Using Client Credentials Flow");
        response.put("endpoints", new String[]{
            "/api/auth/test",
            "/api/test",
            "/api/account/{id}"
        });
        return response;
    }
}
