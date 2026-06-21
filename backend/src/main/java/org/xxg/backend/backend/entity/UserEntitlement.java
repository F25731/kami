package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class UserEntitlement {
    private Long id;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("entitlement_type")
    private String entitlementType;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("remaining_count")
    private Integer remainingCount;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("expire_time")
    private LocalDateTime expireTime;

    @JsonProperty("is_permanent")
    private Boolean isPermanent;

    @JsonProperty("source_card_id")
    private Long sourceCardId;

    @JsonProperty("source_order_no")
    private String sourceOrderNo;

    private String status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEntitlementType() { return entitlementType; }
    public void setEntitlementType(String entitlementType) { this.entitlementType = entitlementType; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getRemainingCount() { return remainingCount; }
    public void setRemainingCount(Integer remainingCount) { this.remainingCount = remainingCount; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public Boolean getIsPermanent() { return isPermanent; }
    public void setIsPermanent(Boolean isPermanent) { this.isPermanent = isPermanent; }

    public Long getSourceCardId() { return sourceCardId; }
    public void setSourceCardId(Long sourceCardId) { this.sourceCardId = sourceCardId; }

    public String getSourceOrderNo() { return sourceOrderNo; }
    public void setSourceOrderNo(String sourceOrderNo) { this.sourceOrderNo = sourceOrderNo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
