package com.salesforce.integration.model;

import java.sql.Date;
import java.util.List;
import java.util.Map;

public class EstimateInfo {
    private String id;
    private String name;
    private String template;
    private Date startdate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    public String getStartdate() { return startdate; }
    public void setStartdate(String startdate) { this.startdate = startdate; }
}