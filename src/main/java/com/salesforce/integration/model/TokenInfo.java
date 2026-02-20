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
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("signature")
    private String signature;
    
    @JsonProperty("scope")
    private String scope;

    public boolean isExpired() {
        // 提前5分钟过期，确保令牌在使用时有效
        return System.currentTimeMillis() - issuedAt > (expiresIn - 300) * 1000;
    }

    // Getters and Setters
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

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    @Override
    public String toString() {
        return "TokenInfo{" +
                "accessToken='[PROTECTED]'" +
                ", instanceUrl='" + instanceUrl + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", issuedAt=" + issuedAt +
                ", refreshToken='[PROTECTED]'" +
                '}';
    }
}
