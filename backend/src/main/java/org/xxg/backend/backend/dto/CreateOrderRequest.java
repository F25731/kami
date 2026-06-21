package org.xxg.backend.backend.dto;

public class CreateOrderRequest {
    private Integer userId;
    private String username;
    private String cardType;
    private String cardSpec;
    private Integer quantity;
    private String email;
    private Integer pricingId;
    private String source;
    private String externalOrderNo;
    private Long packageId;

    public Integer getPricingId() { return pricingId; }
    public void setPricingId(Integer pricingId) { this.pricingId = pricingId; }
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
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalOrderNo() { return externalOrderNo; }
    public void setExternalOrderNo(String externalOrderNo) { this.externalOrderNo = externalOrderNo; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
}