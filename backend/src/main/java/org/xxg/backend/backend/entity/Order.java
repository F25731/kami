package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private Integer id;

    @JsonProperty("order_id")
    private String orderNo;

    @JsonProperty("project_id")
    private Long projectId;

    @JsonProperty("user_id")
    private Integer userId;

    private String username;

    @JsonProperty("card_type")
    private String cardType;

    @JsonProperty("card_spec")
    private String cardSpec;

    private Integer quantity;

    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    @JsonProperty("total_price")
    private BigDecimal totalPrice;

    private String status;
    private String source;

    @JsonProperty("external_order_no")
    private String externalOrderNo;

    @JsonProperty("package_id")
    private Long packageId;

    @JsonProperty("card_keys")
    private String cardKeys;

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonProperty("finish_time")
    private LocalDateTime payTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getCardSpec() { return cardSpec; }
    public void setCardSpec(String cardSpec) { this.cardSpec = cardSpec; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalOrderNo() { return externalOrderNo; }
    public void setExternalOrderNo(String externalOrderNo) { this.externalOrderNo = externalOrderNo; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getCardKeys() { return cardKeys; }
    public void setCardKeys(String cardKeys) { this.cardKeys = cardKeys; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
}