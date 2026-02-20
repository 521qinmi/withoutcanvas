package com.example.sfdc.controller;

import com.example.sfdc.model.OAuthTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/oauth")
public class AuthController {

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.client-secret}")
    private String clientSecret;

    @Value("${salesforce.redirect-uri}")
    private String redirectUri;

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String url = loginUrl + "/services/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=api refresh_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(java.net.URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public OAuthTokenResponse callback(@RequestParam("code") String code) {
        String tokenUrl = loginUrl + "/services/oauth2/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForObject(tokenUrl, body, OAuthTokenResponse.class);
    }
}
