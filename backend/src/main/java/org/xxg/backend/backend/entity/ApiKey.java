package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class ApiKey {
    private Long id;

    @JsonProperty("project_id")
    private Long projectId;

    private String keyName;
    private String apiKey;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiSecret;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiSecretHash;

    private String keyValue;
    private String name;
    private String description;
    private Integer status;
    private String permissions;

    @JsonProperty("allowed_ips")
    private String allowedIps;

    private String environment;

    @JsonProperty("expired_at")
    private LocalDateTime expiredAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastUseTime;
    private Integer useCount;

    private List<User> assignedUsers;
    private Integer userCount;
    private Integer cardCount;

    @JsonProperty("webhook_config")
    private String webhookConfig;

    @JsonProperty("enable_card_encryption")
    private Boolean enableCardEncryption;

    @JsonProperty("require_machine_code")
    private Boolean requireMachineCode;

    @JsonProperty("machine_spec_once_config")
    private String machineSpecOnceConfig;

    public ApiKey() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getKeyName() { return keyName; }
    public void setKeyName(String keyName) { this.keyName = keyName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getApiSecretHash() { return apiSecretHash; }
    public void setApiSecretHash(String apiSecretHash) { this.apiSecretHash = apiSecretHash; }

    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public String getAllowedIps() { return allowedIps; }
    public void setAllowedIps(String allowedIps) { this.allowedIps = allowedIps; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public LocalDateTime getLastUseTime() { return lastUseTime; }
    public void setLastUseTime(LocalDateTime lastUseTime) { this.lastUseTime = lastUseTime; }

    public Integer getUseCount() { return useCount; }
    public void setUseCount(Integer useCount) { this.useCount = useCount; }

    public List<User> getAssignedUsers() { return assignedUsers; }
    public void setAssignedUsers(List<User> assignedUsers) { this.assignedUsers = assignedUsers; }

    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }

    public Integer getCardCount() { return cardCount; }
    public void setCardCount(Integer cardCount) { this.cardCount = cardCount; }

    public String getWebhookConfig() { return webhookConfig; }
    public void setWebhookConfig(String webhookConfig) { this.webhookConfig = webhookConfig; }

    public Boolean getEnableCardEncryption() { return enableCardEncryption; }
    public void setEnableCardEncryption(Boolean enableCardEncryption) { this.enableCardEncryption = enableCardEncryption; }

    public Boolean getRequireMachineCode() { return requireMachineCode; }
    public void setRequireMachineCode(Boolean requireMachineCode) { this.requireMachineCode = requireMachineCode; }

    public String getMachineSpecOnceConfig() { return machineSpecOnceConfig; }
    public void setMachineSpecOnceConfig(String machineSpecOnceConfig) { this.machineSpecOnceConfig = machineSpecOnceConfig; }
}
