package com.salesforce.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenInfo {
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("instance_url")
    private String instanceUrl;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private int expiresIn;
    
    @JsonProperty("issued_at")
    private long issuedAt;

    public boolean isExpired() {
        return System.currentTimeMillis() - issuedAt > (expiresIn - 300) * 1000;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getInstanceUrl() { return instanceUrl; }
    public void setInstanceUrl(String instanceUrl) { this.instanceUrl = instanceUrl; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }

    public long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(long issuedAt) { this.issuedAt = issuedAt; }
}