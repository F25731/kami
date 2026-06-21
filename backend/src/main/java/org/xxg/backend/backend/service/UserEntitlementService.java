package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.CardPackage;
import org.xxg.backend.backend.entity.UserEntitlement;
import org.xxg.backend.backend.mapper.ConsumeLogMapper;
import org.xxg.backend.backend.mapper.UserEntitlementMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserEntitlementService {

    private final UserEntitlementMapper entitlementMapper;
    private final ConsumeLogMapper consumeLogMapper;
    private final ProjectWebhookService projectWebhookService;

    public UserEntitlementService(UserEntitlementMapper entitlementMapper,
                                  ConsumeLogMapper consumeLogMapper,
                                  ProjectWebhookService projectWebhookService) {
        this.entitlementMapper = entitlementMapper;
        this.consumeLogMapper = consumeLogMapper;
        this.projectWebhookService = projectWebhookService;
    }

    public Map<String, Object> getStatus(Long projectId, String userId, String entitlementType) {
        Map<String, Object> result = new HashMap<>();
        UserEntitlement ent = entitlementMapper.findActiveByProjectIdAndUserIdAndType(
            projectId, userId, entitlementType);
        if (ent == null) {
            result.put("success", true);
            result.put("has_entitlement", false);
            result.put("remaining_count", 0);
            return result;
        }
        result.put("success", true);
        result.put("has_entitlement", true);
        result.put("entitlement_id", ent.getId());
        result.put("entitlement_type", ent.getEntitlementType());
        result.put("total_count", ent.getTotalCount());
        result.put("remaining_count", ent.getRemainingCount());
        result.put("is_permanent", ent.getIsPermanent());
        result.put("start_time", ent.getStartTime());
        result.put("expire_time", ent.getExpireTime());
        result.put("status", ent.getStatus());
        return result;
    }

    /**
     * Redeem a card into a user entitlement.
     * Called by CardService after card is verified and about to be consumed for redeem_to_account mode.
     */
    public UserEntitlement redeemFromPackage(Long projectId, String userId, CardPackage pkg,
                                              Long cardId, String orderNo) {
        UserEntitlement ent = new UserEntitlement();
        ent.setProjectId(projectId);
        ent.setUserId(userId);
        ent.setEntitlementType(pkg.getPackageCode());
        ent.setTotalCount(pkg.getCountValue() != null ? pkg.getCountValue() : 0);
        ent.setRemainingCount(pkg.getCountValue() != null ? pkg.getCountValue() : 0);
        ent.setIsPermanent(Boolean.TRUE.equals(pkg.getIsPermanent()));
        ent.setSourceCardId(cardId);
        ent.setSourceOrderNo(orderNo);
        ent.setStatus("active");

        LocalDateTime now = LocalDateTime.now();
        ent.setStartTime(now);
        if (!Boolean.TRUE.equals(pkg.getIsPermanent()) && pkg.getDurationDays() != null && pkg.getDurationDays() > 0) {
            ent.setExpireTime(now.plusDays(pkg.getDurationDays()));
        }

        Long id = entitlementMapper.insert(ent);
        ent.setId(id);
        return ent;
    }

    /**
     * Consume N units from an entitlement (idempotent via biz_id).
     */
    public Map<String, Object> consume(Long projectId, String userId, String entitlementType,
                                        int amount, String bizId, String clientIp) {
        Map<String, Object> result = new HashMap<>();

        UserEntitlement ent = entitlementMapper.findActiveByProjectIdAndUserIdAndType(
            projectId, userId, entitlementType);
        if (ent == null) {
            result.put("success", false);
            result.put("code", "NO_ENTITLEMENT");
            result.put("message", "用户无有效权益");
            return result;
        }

        // Idempotency check
        if (bizId != null && !bizId.isBlank()) {
            Map<String, Object> existingLog = consumeLogMapper.findEntitlementConsumeLogByBizId(projectId, bizId);
            if (existingLog != null) {
                result.put("success", true);
                result.put("idempotent", true);
                result.put("remaining_count", ent.getRemainingCount());
                result.put("message", "重复请求，已忽略");
                return result;
            }
        }

        if (!Boolean.TRUE.equals(ent.getIsPermanent()) && ent.getRemainingCount() < amount) {
            result.put("success", false);
            result.put("code", "INSUFFICIENT");
            result.put("message", "权益次数不足，剩余: " + ent.getRemainingCount());
            return result;
        }

        int updated = 0;
        if (!Boolean.TRUE.equals(ent.getIsPermanent())) {
            updated = entitlementMapper.decrementRemainingCount(ent.getId(), amount, amount);
            if (updated == 0) {
                result.put("success", false);
                result.put("code", "CONCURRENT_CONFLICT");
                result.put("message", "权益次数不足或并发冲突，请重试");
                return result;
            }
        }

        consumeLogMapper.insertEntitlementConsumeLog(projectId, ent.getId(), bizId, userId, amount, clientIp);

        int newRemaining = Boolean.TRUE.equals(ent.getIsPermanent())
            ? ent.getRemainingCount()
            : ent.getRemainingCount() - amount;

        // WebHook: entitlement.consumed
        if (projectWebhookService != null) {
            Map<String, Object> webhookData = new HashMap<>();
            webhookData.put("user_id", userId);
            webhookData.put("entitlement_id", ent.getId());
            webhookData.put("entitlement_type", entitlementType);
            webhookData.put("amount", amount);
            webhookData.put("before_count", ent.getRemainingCount());
            webhookData.put("after_count", newRemaining);
            webhookData.put("biz_id", bizId);
            webhookData.put("client_ip", clientIp);
            projectWebhookService.trigger(projectId, "entitlement.consumed", webhookData);
        }

        result.put("success", true);
        result.put("entitlement_id", ent.getId());
        result.put("remaining_count", newRemaining);
        result.put("is_permanent", ent.getIsPermanent());
        return result;
    }

    /**
     * Refund N units back to entitlement.
     */
    public Map<String, Object> refundConsume(Long projectId, String userId, String entitlementType,
                                              int amount, String bizId) {
        Map<String, Object> result = new HashMap<>();

        UserEntitlement ent = entitlementMapper.findActiveByProjectIdAndUserIdAndType(
            projectId, userId, entitlementType);
        if (ent == null) {
            result.put("success", false);
            result.put("code", "NO_ENTITLEMENT");
            result.put("message", "用户无有效权益");
            return result;
        }

        // Verify the original consume log exists and is not already refunded
        Map<String, Object> log = consumeLogMapper.findEntitlementConsumeLogByBizId(projectId, bizId);
        if (log == null) {
            result.put("success", false);
            result.put("code", "LOG_NOT_FOUND");
            result.put("message", "未找到对应的消费记录");
            return result;
        }
        Object refunded = log.get("refunded");
        if (refunded instanceof Number && ((Number) refunded).intValue() == 1) {
            result.put("success", false);
            result.put("code", "ALREADY_REFUNDED");
            result.put("message", "该消费记录已退还");
            return result;
        }

        if (!Boolean.TRUE.equals(ent.getIsPermanent())) {
            entitlementMapper.incrementRemainingCount(ent.getId(), amount);
        }

        Long logId = ((Number) log.get("id")).longValue();
        consumeLogMapper.markEntitlementConsumeRefunded(logId);

        result.put("success", true);
        result.put("message", "权益已退还");
        return result;
    }

    public List<UserEntitlement> listByProjectId(Long projectId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return entitlementMapper.findByProjectId(projectId, offset, pageSize);
    }

    public int countByProjectId(Long projectId) {
        return entitlementMapper.countByProjectId(projectId);
    }
}
