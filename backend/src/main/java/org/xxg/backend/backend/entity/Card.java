package org.xxg.backend.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * 鍗″瘑瀹炰綋绫?
 */
public class Card {
    private Long id;

    @JsonProperty("project_id")
    private Long projectId;
    @JsonProperty("card_key")
    private String cardKey;
    
    @JsonProperty("encrypted_key")
    private String encryptedKey;
    
    private Integer status; // 0:鏈娇鐢?1:宸蹭娇鐢?2:宸叉殏鍋?绠＄悊鍛? 4:宸插悎骞?鏃堕棿鍗＄画鏈熷埌鍏跺畠鍗?
    
    @JsonProperty("create_time")
    private LocalDateTime createTime;
    
    @JsonProperty("use_time")
    private LocalDateTime useTime;
    
    @JsonProperty("expire_time")
    private LocalDateTime expireTime;
    
    private Integer duration;
    
    @JsonProperty("verify_method")
    private String verifyMethod;
    
    @JsonProperty("allow_reverify")
    private Integer allowReverify;
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("ip_address")
    private String ipAddress;
    
    @JsonProperty("encryption_type")
    private String encryptionType;
    
    @JsonProperty("card_type")
    private String cardType;
    
    @JsonProperty("total_count")
    private Integer totalCount;
    
    @JsonProperty("remaining_count")
    private Integer remainingCount;
    
    @JsonProperty("creator_type")
    private String creatorType;
    
    @JsonProperty("creator_id")
    private Long creatorId;
    
    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("api_key_id")
    private Long apiKeyId;

    @JsonProperty("machine_code")
    private String machineCode;

    /** true锛氬悓鏈哄櫒鐮佷笂鑻ュ凡鏈夋湭杩囨湡鏃堕棿鍗★紝鏍搁攢鏈崱鏃跺皢鏃堕暱鍙犲姞鍒拌鍗＄殑鍒版湡鏃堕棿锛堝崟寮?false 鏃舵部鐢ㄧ幇琛屻€屼粠褰撳墠鏃跺埢璧风畻銆嶉€昏緫锛?*/
    @JsonProperty("stack_time_if_same_machine")
    private Boolean stackTimeIfSameMachine;

    /** true锛氬厑璁告寔鍗＄敤鎴峰湪棣栭〉銆屽湪绾胯В缁戙€嶈嚜鍔╂竻绌烘満鍣ㄧ爜涓庤澶囩粦瀹?*/
    @JsonProperty("allow_self_unbind")
    private Boolean allowSelfUnbind;

    /** 鑻ヤ负宸插悎骞跺崱锛屾寚鍚戣缁湡鐨勪富鍗?rows */
    @JsonProperty("merged_into_card_id")
    private Long mergedIntoCardId;

    // ─── multi-project new fields ───────────────────────────────────────────

    @JsonProperty("package_id")
    private Long packageId;

    @JsonProperty("order_no")
    private String orderNo;

    private String source;  // api / admin / import

    @JsonProperty("bind_device_id")
    private String bindDeviceId;

    @JsonProperty("bind_time")
    private LocalDateTime bindTime;

    @JsonProperty("bind_type")
    private String bindType;  // machine_code / android_id / custom

    @JsonProperty("redeemed_user_id")
    private String redeemedUserId;

    @JsonProperty("redeemed_at")
    private LocalDateTime redeemedAt;

    @JsonProperty("activated_at")
    private LocalDateTime activatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getCardKey() { return cardKey; }
    public void setCardKey(String cardKey) { this.cardKey = cardKey; }

    public String getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(String encryptedKey) { this.encryptedKey = encryptedKey; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUseTime() { return useTime; }
    public void setUseTime(LocalDateTime useTime) { this.useTime = useTime; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getVerifyMethod() { return verifyMethod; }
    public void setVerifyMethod(String verifyMethod) { this.verifyMethod = verifyMethod; }

    public Integer getAllowReverify() { return allowReverify; }
    public void setAllowReverify(Integer allowReverify) { this.allowReverify = allowReverify; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getEncryptionType() { return encryptionType; }
    public void setEncryptionType(String encryptionType) { this.encryptionType = encryptionType; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getRemainingCount() { return remainingCount; }
    public void setRemainingCount(Integer remainingCount) { this.remainingCount = remainingCount; }

    public String getCreatorType() { return creatorType; }
    public void setCreatorType(String creatorType) { this.creatorType = creatorType; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public Long getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Long apiKeyId) { this.apiKeyId = apiKeyId; }

    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }

    public Boolean getStackTimeIfSameMachine() { return stackTimeIfSameMachine; }
    public void setStackTimeIfSameMachine(Boolean stackTimeIfSameMachine) { this.stackTimeIfSameMachine = stackTimeIfSameMachine; }

    public Boolean getAllowSelfUnbind() { return allowSelfUnbind; }
    public void setAllowSelfUnbind(Boolean allowSelfUnbind) { this.allowSelfUnbind = allowSelfUnbind; }

    public Long getMergedIntoCardId() { return mergedIntoCardId; }
    public void setMergedIntoCardId(Long mergedIntoCardId) { this.mergedIntoCardId = mergedIntoCardId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getBindDeviceId() { return bindDeviceId; }
    public void setBindDeviceId(String bindDeviceId) { this.bindDeviceId = bindDeviceId; }

    public LocalDateTime getBindTime() { return bindTime; }
    public void setBindTime(LocalDateTime bindTime) { this.bindTime = bindTime; }

    public String getBindType() { return bindType; }
    public void setBindType(String bindType) { this.bindType = bindType; }

    public String getRedeemedUserId() { return redeemedUserId; }
    public void setRedeemedUserId(String redeemedUserId) { this.redeemedUserId = redeemedUserId; }

    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
}

