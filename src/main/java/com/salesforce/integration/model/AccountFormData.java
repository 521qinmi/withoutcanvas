package com.salesforce.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountFormData {
    private String sfRecordId;
    private String accountName;
    private String accountNumber;
    private String phone;
    private String address;
    private String industry;
    private String annualRevenue;
    private String numberOfEmployees;
    private String description;
    private String website;
    private String billingAddress;
    private String shippingAddress;
    private String ownerName;
    private String createdDate;
    private String lastModifiedDate;
    
    // Getters and Setters
    
    public String getSfRecordId() {
        return sfRecordId;
    }
    
    public void setSfRecordId(String sfRecordId) {
        this.sfRecordId = sfRecordId;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getIndustry() {
        return industry;
    }
    
    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    public String getAnnualRevenue() {
        return annualRevenue;
    }
    
    public void setAnnualRevenue(String annualRevenue) {
        this.annualRevenue = annualRevenue;
    }
    
    public String getNumberOfEmployees() {
        return numberOfEmployees;
    }
    
    public void setNumberOfEmployees(String numberOfEmployees) {
        this.numberOfEmployees = numberOfEmployees;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public String getBillingAddress() {
        return billingAddress;
    }
    
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
    
    public String getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}