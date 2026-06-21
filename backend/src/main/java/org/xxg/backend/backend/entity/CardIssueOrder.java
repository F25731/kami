package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class CardIssueOrder {
    private Long id;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("order_no")
    private String orderNo;

    @JsonProperty("package_id")
    private Long packageId;

    private Integer quantity;

    @JsonProperty("card_type")
    private String cardType;

    @JsonProperty("count_value")
    private Integer countValue;

    @JsonProperty("duration_days")
    private Integer durationDays;

    @JsonProperty("is_permanent")
    private Boolean isPermanent;

    private String remark;
    private String status;

    @JsonProperty("operator_id")
    private Long operatorId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public Integer getCountValue() { return countValue; }
    public void setCountValue(Integer countValue) { this.countValue = countValue; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Boolean getIsPermanent() { return isPermanent; }
    public void setIsPermanent(Boolean isPermanent) { this.isPermanent = isPermanent; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
