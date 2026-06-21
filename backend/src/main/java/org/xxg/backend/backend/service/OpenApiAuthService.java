package org.xxg.backend.backend.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.entity.ProjectApiKey;
import org.xxg.backend.backend.mapper.ProjectApiKeyMapper;
import org.xxg.backend.backend.util.SignatureUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates project_token, API Key, IP whitelist, HMAC signature, timestamp freshness, and nonce replay.
 */
@Service
public class OpenApiAuthService {

    private static final int TIMESTAMP_TOLERANCE_SECONDS = 300;  // ±5 min
    private static final String NONCE_REDIS_PREFIX = "kami:nonce:";

    private final ProjectService projectService;
    private final ProjectApiKeyMapper keyMapper;
    private final ProjectApiKeyService keyService;
    private final StringRedisTemplate redisTemplate;

    public OpenApiAuthService(ProjectService projectService,
                               ProjectApiKeyMapper keyMapper,
                               ProjectApiKeyService keyService,
                               StringRedisTemplate redisTemplate) {
        this.projectService = projectService;
        this.keyMapper = keyMapper;
        this.keyService = keyService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Full authentication pipeline.
     *
     * @param projectToken  7-char project token from URL
     * @param apiKey        X-API-KEY header
     * @param timestamp     X-TIMESTAMP header (Unix seconds string)
     * @param nonce         X-NONCE header
     * @param signature     X-SIGN header
     * @param rawBody       request body string (may be empty)
     * @param clientIp      caller IP
     * @param requiredPerm  permission name, e.g. "cards:consume"
     * @return map with "success", "project" (Project), "apiKey" (ProjectApiKey), or "message" on failure
     */
    public Map<String, Object> authenticate(String projectToken, String apiKey, String timestamp,
                                             String nonce, String signature, String rawBody,
                                             String clientIp, String requiredPerm) {
        Map<String, Object> result = new HashMap<>();

        // 1. Resolve project
        Project project = projectService.getByToken(projectToken);
        if (project == null || !"active".equals(project.getStatus())) {
            return fail(result, "PROJECT_NOT_FOUND", "项目不存在或已停用");
        }

        // 2. Required headers present
        if (isBlank(apiKey) || isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            return fail(result, "MISSING_HEADERS", "缺少必要的请求头: X-API-KEY, X-TIMESTAMP, X-NONCE, X-SIGN");
        }

        // 3. Timestamp freshness
        long reqTime;
        try {
            reqTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return fail(result, "INVALID_TIMESTAMP", "X-TIMESTAMP 格式无效");
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - reqTime) > TIMESTAMP_TOLERANCE_SECONDS) {
            return fail(result, "TIMESTAMP_EXPIRED", "请求时间戳已过期（±5分钟内有效）");
        }

        // 4. Nonce replay check (TTL = 2× tolerance)
        String nonceKey = NONCE_REDIS_PREFIX + project.getId() + ":" + nonce;
        Boolean nonceUsed = redisTemplate.hasKey(nonceKey);
        if (Boolean.TRUE.equals(nonceUsed)) {
            return fail(result, "NONCE_REPLAYED", "Nonce 已被使用，请勿重放请求");
        }

        // 5. Look up API key (must belong to this project and be active)
        ProjectApiKey key = keyService.findActiveKeyForProject(apiKey, project.getId());
        if (key == null) {
            return fail(result, "INVALID_API_KEY", "API Key 无效或已停用");
        }

        // 6. IP whitelist
        if (!isIpAllowed(clientIp, key.getAllowedIps())) {
            return fail(result, "IP_BLOCKED", "客户端 IP 不在白名单中: " + clientIp);
        }

        // 7. Signature verification. The api_secret returned at creation is the stored signing secret.
        String expectedSign = SignatureUtil.generateSignature(apiKey, timestamp, nonce,
            rawBody != null ? rawBody : "", key.getApiSecretHash());
        if (!expectedSign.equals(signature)) {
            return fail(result, "SIGNATURE_INVALID", "签名验证失败");
        }

        // 8. Permission check
        if (requiredPerm != null && !keyService.hasPermission(key, requiredPerm)) {
            return fail(result, "FORBIDDEN", "API Key 无权限: " + requiredPerm);
        }

        // 9. Mark nonce as used (TTL slightly longer than tolerance window)
        redisTemplate.opsForValue().set(nonceKey, "1", Duration.ofSeconds(TIMESTAMP_TOLERANCE_SECONDS * 3));

        // 10. Async increment use count (best-effort, fire and forget)
        keyMapper.incrementUseCount(apiKey);

        result.put("success", true);
        result.put("project", project);
        result.put("apiKey", key);
        return result;
    }

    private boolean isIpAllowed(String clientIp, String allowedIps) {
        if (allowedIps == null || allowedIps.isBlank()) return true;  // no restriction
        List<String> allowed = List.of(allowedIps.split("[,\\s]+"));
        return allowed.stream().anyMatch(ip -> ip.equals(clientIp) || ip.equals("*"));
    }

    private Map<String, Object> fail(Map<String, Object> result, String code, String message) {
        result.put("success", false);
        result.put("code", code);
        result.put("message", message);
        return result;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
