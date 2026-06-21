package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class Project {
    private Long id;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("project_code")
    private String projectCode;

    @JsonProperty("project_token")
    private String projectToken;

    @JsonProperty("project_type")
    private String projectType;

    private String environment;

    @JsonProperty("usage_mode")
    private String usageMode;

    private String status;
    private String remark;

    @JsonProperty("enable_device_bind")
    private Boolean enableDeviceBind;

    @JsonProperty("device_bind_mode")
    private String deviceBindMode;

    @JsonProperty("enable_signature")
    private Boolean enableSignature;

    @JsonProperty("enable_ip_whitelist")
    private Boolean enableIpWhitelist;

    @JsonProperty("rate_limit_per_minute")
    private Integer rateLimitPerMinute;

    @JsonProperty("max_generate_per_request")
    private Integer maxGeneratePerRequest;

    @JsonProperty("max_generate_per_day")
    private Integer maxGeneratePerDay;

    @JsonProperty("webhook_enabled")
    private Boolean webhookEnabled;

    @JsonProperty("webhook_url")
    private String webhookUrl;

    @JsonProperty("webhook_secret")
    private String webhookSecret;

    @JsonProperty("webhook_events")
    private String webhookEvents;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("card_count")
    private Integer cardCount;

    @JsonProperty("pricing_count")
    private Integer pricingCount;

    @JsonProperty("api_key_count")
    private Integer apiKeyCount;

    @JsonProperty("today_call_count")
    private Integer todayCallCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getProjectToken() { return projectToken; }
    public void setProjectToken(String projectToken) { this.projectToken = projectToken; }

    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getUsageMode() { return usageMode; }
    public void setUsageMode(String usageMode) { this.usageMode = usageMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Boolean getEnableDeviceBind() { return enableDeviceBind; }
    public void setEnableDeviceBind(Boolean enableDeviceBind) { this.enableDeviceBind = enableDeviceBind; }

    public String getDeviceBindMode() { return deviceBindMode; }
    public void setDeviceBindMode(String deviceBindMode) { this.deviceBindMode = deviceBindMode; }

    public Boolean getEnableSignature() { return enableSignature; }
    public void setEnableSignature(Boolean enableSignature) { this.enableSignature = enableSignature; }

    public Boolean getEnableIpWhitelist() { return enableIpWhitelist; }
    public void setEnableIpWhitelist(Boolean enableIpWhitelist) { this.enableIpWhitelist = enableIpWhitelist; }

    public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public Integer getMaxGeneratePerRequest() { return maxGeneratePerRequest; }
    public void setMaxGeneratePerRequest(Integer maxGeneratePerRequest) { this.maxGeneratePerRequest = maxGeneratePerRequest; }

    public Integer getMaxGeneratePerDay() { return maxGeneratePerDay; }
    public void setMaxGeneratePerDay(Integer maxGeneratePerDay) { this.maxGeneratePerDay = maxGeneratePerDay; }

    public Boolean getWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(Boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getWebhookEvents() { return webhookEvents; }
    public void setWebhookEvents(String webhookEvents) { this.webhookEvents = webhookEvents; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getCardCount() { return cardCount; }
    public void setCardCount(Integer cardCount) { this.cardCount = cardCount; }

    public Integer getPricingCount() { return pricingCount; }
    public void setPricingCount(Integer pricingCount) { this.pricingCount = pricingCount; }

    public Integer getApiKeyCount() { return apiKeyCount; }
    public void setApiKeyCount(Integer apiKeyCount) { this.apiKeyCount = apiKeyCount; }

    public Integer getTodayCallCount() { return todayCallCount; }
    public void setTodayCallCount(Integer todayCallCount) { this.todayCallCount = todayCallCount; }
}
