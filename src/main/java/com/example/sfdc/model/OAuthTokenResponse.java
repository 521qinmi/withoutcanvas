package com.example.sfdc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OAuthTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("instance_url")
    private String instanceUrl;

    @JsonProperty("refresh_token")
    private String refreshToken;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getInstanceUrl() { return instanceUrl; }
    public void setInstanceUrl(String instanceUrl) { this.instanceUrl = instanceUrl; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
