package org.xxg.backend.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.entity.ProjectApiKey;
import org.xxg.backend.backend.service.*;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Project-scoped open API endpoints.
 * URL pattern: /api/p/{project_token}/(cards|entitlements)/...
 *
 * Auth headers: X-API-KEY, X-TIMESTAMP, X-NONCE, X-SIGN
 */
@RestController
@RequestMapping("/api/p/{project_token}")
public class ProjectOpenApiController {

    private final OpenApiAuthService authService;
    private final CardService cardService;
    private final UserEntitlementService entitlementService;
    private final ApiCallLogService logService;
    private final ObjectMapper objectMapper;

    public ProjectOpenApiController(OpenApiAuthService authService,
                                    CardService cardService,
                                    UserEntitlementService entitlementService,
                                    ApiCallLogService logService,
                                    ObjectMapper objectMapper) {
        this.authService = authService;
        this.cardService = cardService;
        this.entitlementService = entitlementService;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. cards/generate  (POST)  — generate cards via API
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/generate")
    public ResponseEntity<Map<String, Object>> generateCards(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:generate");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String orderNo = str(params, "order_no");
        Integer quantity = intVal(params, "quantity", 1);
        String cardType = str(params, "card_type");
        Integer countValue = intVal(params, "count_value", 0);
        Integer durationDays = intVal(params, "duration_days", 0);
        Boolean isPermanent = boolVal(params, "is_permanent");
        Long packageId = longVal(params, "package_id");
        String remark = str(params, "remark");

        Map<String, Object> result = cardService.generateViaApi(
            project.getId(), orderNo, quantity, cardType, countValue,
            durationDays, isPermanent, packageId, remark);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. cards/verify  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/verify")
    public ResponseEntity<Map<String, Object>> verifyCard(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:verify");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        if (cardKey == null || cardKey.isBlank()) return badRequest("card_key 不能为空");

        Map<String, Object> result = cardService.verifyForOpenApi(project.getId(), cardKey);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. cards/status  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/status")
    public ResponseEntity<Map<String, Object>> cardStatus(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:status");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        if (cardKey == null || cardKey.isBlank()) return badRequest("card_key 不能为空");

        Map<String, Object> result = cardService.getStatusForOpenApi(project.getId(), cardKey);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. cards/consume  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/consume")
    public ResponseEntity<Map<String, Object>> consumeCard(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:consume");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        String bizId = str(params, "biz_id");
        String deviceId = str(params, "device_id");
        Integer amount = intVal(params, "amount", 1);

        if (cardKey == null || cardKey.isBlank()) return badRequest("card_key 不能为空");

        Map<String, Object> result = cardService.consumeForOpenApi(
            project.getId(), cardKey, bizId, deviceId, amount, getClientIp(req));
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  5. cards/redeem  (POST)  — redeem card key → user entitlement
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/redeem")
    public ResponseEntity<Map<String, Object>> redeemCard(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:redeem");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        String userId = str(params, "user_id");

        if (cardKey == null || cardKey.isBlank()) return badRequest("card_key 不能为空");
        if (userId == null || userId.isBlank()) return badRequest("user_id 不能为空");

        Map<String, Object> result = cardService.redeemForOpenApi(
            project.getId(), cardKey, userId, getClientIp(req));
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  6. cards/unbind-device  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/unbind-device")
    public ResponseEntity<Map<String, Object>> unbindDevice(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:unbind_device");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        String reason = str(params, "reason");

        if (cardKey == null || cardKey.isBlank()) return badRequest("card_key 不能为空");

        Map<String, Object> result = cardService.unbindDeviceForOpenApi(
            project.getId(), cardKey, reason, getClientIp(req));
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  7. cards/refund-consume  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/cards/refund-consume")
    public ResponseEntity<Map<String, Object>> refundConsume(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "cards:refund_consume");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String cardKey = str(params, "card_key");
        String bizId = str(params, "biz_id");

        if (bizId == null || bizId.isBlank()) return badRequest("biz_id 不能为空");

        Map<String, Object> result = cardService.refundConsumeForOpenApi(
            project.getId(), cardKey, bizId);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  8. entitlements/status  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/entitlements/status")
    public ResponseEntity<Map<String, Object>> entitlementStatus(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "entitlements:status");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String userId = str(params, "user_id");
        String entitlementType = str(params, "entitlement_type");

        if (userId == null || userId.isBlank()) return badRequest("user_id 不能为空");
        if (entitlementType == null || entitlementType.isBlank()) return badRequest("entitlement_type 不能为空");

        Map<String, Object> result = entitlementService.getStatus(
            project.getId(), userId, entitlementType);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  9. entitlements/consume  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/entitlements/consume")
    public ResponseEntity<Map<String, Object>> entitlementConsume(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "entitlements:consume");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String userId = str(params, "user_id");
        String entitlementType = str(params, "entitlement_type");
        Integer amount = intVal(params, "amount", 1);
        String bizId = str(params, "biz_id");

        if (userId == null || userId.isBlank()) return badRequest("user_id 不能为空");
        if (entitlementType == null || entitlementType.isBlank()) return badRequest("entitlement_type 不能为空");

        Map<String, Object> result = entitlementService.consume(
            project.getId(), userId, entitlementType, amount, bizId, getClientIp(req));
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  10. entitlements/refund-consume  (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/entitlements/refund-consume")
    public ResponseEntity<Map<String, Object>> entitlementRefundConsume(
            @PathVariable("project_token") String projectToken,
            HttpServletRequest req) throws Exception {

        String body = readBody(req);
        Map<String, Object> authResult = authenticate(projectToken, req, body, "entitlements:refund_consume");
        if (!isOk(authResult)) return unauthorized(authResult);

        Project project = (Project) authResult.get("project");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = objectMapper.readValue(body, Map.class);

        String userId = str(params, "user_id");
        String entitlementType = str(params, "entitlement_type");
        Integer amount = intVal(params, "amount", 1);
        String bizId = str(params, "biz_id");

        if (userId == null || userId.isBlank()) return badRequest("user_id 不能为空");
        if (bizId == null || bizId.isBlank()) return badRequest("biz_id 不能为空");

        Map<String, Object> result = entitlementService.refundConsume(
            project.getId(), userId, entitlementType, amount, bizId);
        return ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> authenticate(String projectToken, HttpServletRequest req,
                                              String body, String permission) {
        return authService.authenticate(
            projectToken,
            req.getHeader("X-API-KEY"),
            req.getHeader("X-TIMESTAMP"),
            req.getHeader("X-NONCE"),
            req.getHeader("X-SIGN"),
            body,
            getClientIp(req),
            permission);
    }

    private String readBody(HttpServletRequest req) throws Exception {
        // If body was cached by filter, read from attribute; otherwise read stream
        Object cached = req.getAttribute("_cached_body");
        if (cached instanceof String) return (String) cached;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        String body = sb.toString();
        req.setAttribute("_cached_body", body);
        return body;
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = req.getHeader("X-Real-IP");
        return realIp != null ? realIp : req.getRemoteAddr();
    }

    private boolean isOk(Map<String, Object> result) {
        return Boolean.TRUE.equals(result.get("success"));
    }

    private ResponseEntity<Map<String, Object>> ok(Map<String, Object> result) {
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> unauthorized(Map<String, Object> authResult) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", authResult.getOrDefault("code", "UNAUTHORIZED"));
        body.put("message", authResult.getOrDefault("message", "鉴权失败"));
        return ResponseEntity.status(401).body(body);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", "BAD_REQUEST");
        body.put("message", message);
        return ResponseEntity.status(400).body(body);
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof String ? ((String) v).trim() : (v != null ? v.toString() : null);
    }

    private Integer intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) try { return Integer.parseInt((String) v); } catch (NumberFormatException ignored) {}
        return def;
    }

    private Long longVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) try { return Long.parseLong((String) v); } catch (NumberFormatException ignored) {}
        return null;
    }

    private Boolean boolVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return "true".equalsIgnoreCase((String) v);
        return null;
    }
}
