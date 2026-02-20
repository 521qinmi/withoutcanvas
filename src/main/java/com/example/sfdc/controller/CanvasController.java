package com.example.sfdc.controller;

import com.example.sfdc.service.SalesforceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canvas")
public class CanvasController {

    private final SalesforceService service;

    public CanvasController(SalesforceService service) {
        this.service = service;
    }

    @GetMapping("/userinfo")
    public String userInfo(@RequestParam String token,
                           @RequestParam String instanceUrl) {
        return service.getUserInfo(token, instanceUrl);
    }
}
