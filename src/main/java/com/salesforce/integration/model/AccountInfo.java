package com.salesforce.integration.model;

import java.util.List;
import java.util.Map;

public class AccountInfo {
    private String id;
    private String name;
    private String phone;
    private String website;
    private String industry;
    private Double annualRevenue;
    private String description;
    private String ownerName;
    private List<Map<String, String>> contacts;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public Double getAnnualRevenue() { return annualRevenue; }
    public void setAnnualRevenue(Double annualRevenue) { this.annualRevenue = annualRevenue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public List<Map<String, String>> getContacts() { return contacts; }
    public void setContacts(List<Map<String, String>> contacts) { this.contacts = contacts; }
}