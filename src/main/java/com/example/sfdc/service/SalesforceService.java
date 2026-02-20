package com.example.sfdc.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SalesforceService {

    public String getUserInfo(String accessToken, String instanceUrl) {
        String url = instanceUrl + "/services/oauth2/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(access(accessToken));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private String access(String token){
        return token;
    }
}
