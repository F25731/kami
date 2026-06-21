package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class ProjectApiKey {
    private Long id;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("key_name")
    private String keyName;

    @JsonProperty("api_key")
    private String apiKey;

    @JsonProperty("api_secret")
    private String apiSecret; // 仅创建时返回

    @JsonProperty("api_secret_hash")
    private String apiSecretHash;

    private String permissions; // JSON格式

    @JsonProperty("allowed_ips")
    private String allowedIps;

    private String status;
    private String environment;

    @JsonProperty("last_used_at")
    private LocalDateTime lastUsedAt;

    @JsonProperty("use_count")
    private Long useCount;

    @JsonProperty("expired_at")
    private LocalDateTime expiredAt;

    private String remark;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
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

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public String getAllowedIps() { return allowedIps; }
    public void setAllowedIps(String allowedIps) { this.allowedIps = allowedIps; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Long getUseCount() { return useCount; }
    public void setUseCount(Long useCount) { this.useCount = useCount; }

    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
