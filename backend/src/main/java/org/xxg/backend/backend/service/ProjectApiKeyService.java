package org.xxg.backend.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.ProjectApiKey;
import org.xxg.backend.backend.mapper.ProjectApiKeyMapper;
import org.xxg.backend.backend.util.SignatureUtil;
import org.xxg.backend.backend.util.TokenGenerator;

import java.util.*;

@Service
public class ProjectApiKeyService {

    private final ProjectApiKeyMapper keyMapper;
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;

    public ProjectApiKeyService(ProjectApiKeyMapper keyMapper,
                                ProjectService projectService,
                                ObjectMapper objectMapper) {
        this.keyMapper = keyMapper;
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }

    public List<ProjectApiKey> listByProjectId(Long projectId) {
        List<ProjectApiKey> keys = keyMapper.findByProjectId(projectId);
        // Never return secret hash to caller
        keys.forEach(k -> k.setApiSecretHash(null));
        return keys;
    }

    public Map<String, Object> create(Long projectId, ProjectApiKey req) {
        Map<String, Object> result = new HashMap<>();

        if (projectService.getById(projectId) == null) {
            result.put("success", false);
            result.put("message", "项目不存在");
            return result;
        }

        String rawApiKey = TokenGenerator.generateApiKey();
        String signingSecret = SignatureUtil.sha256(TokenGenerator.generateApiSecret());
        String secretHash = signingSecret;

        ProjectApiKey key = new ProjectApiKey();
        key.setProjectId(projectId);
        key.setKeyName(req.getKeyName() != null ? req.getKeyName() : "API Key");
        key.setApiKey(rawApiKey);
        key.setApiSecretHash(secretHash);
        key.setPermissions(req.getPermissions() != null ? req.getPermissions() : buildDefaultPermissions());
        key.setAllowedIps(req.getAllowedIps());
        key.setStatus("active");
        key.setEnvironment(req.getEnvironment() != null ? req.getEnvironment() : "production");
        key.setExpiredAt(req.getExpiredAt());
        key.setRemark(req.getRemark());

        Long id = keyMapper.insert(key);

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("api_key", rawApiKey);
        data.put("api_secret", signingSecret);  // only returned once; use this value for request signatures
        data.put("key_name", key.getKeyName());
        data.put("permissions", key.getPermissions());

        result.put("success", true);
        result.put("data", data);
        result.put("message", "请立即保存 api_secret，此后不再显示");
        return result;
    }

    @CacheEvict(value = "apiKeys", allEntries = true)
    public Map<String, Object> rotate(Long id) {
        Map<String, Object> result = new HashMap<>();
        ProjectApiKey key = keyMapper.findById(id);
        if (key == null) {
            result.put("success", false);
            result.put("message", "API Key 不存在");
            return result;
        }

        String newApiKey = TokenGenerator.generateApiKey();
        String newSigningSecret = SignatureUtil.sha256(TokenGenerator.generateApiSecret());
        String newHash = newSigningSecret;

        keyMapper.rotate(id, newApiKey, newHash);

        Map<String, Object> data = new HashMap<>();
        data.put("api_key", newApiKey);
        data.put("api_secret", newSigningSecret);  // only returned once; use this value for request signatures
        result.put("success", true);
        result.put("data", data);
        result.put("message", "API Key 已轮换，请立即保存新的 api_secret");
        return result;
    }

    @CacheEvict(value = "apiKeys", allEntries = true)
    public Map<String, Object> update(Long id, ProjectApiKey req) {
        Map<String, Object> result = new HashMap<>();
        ProjectApiKey key = keyMapper.findById(id);
        if (key == null) {
            result.put("success", false);
            result.put("message", "API Key 不存在");
            return result;
        }
        if (req.getKeyName() != null) key.setKeyName(req.getKeyName());
        if (req.getPermissions() != null) key.setPermissions(req.getPermissions());
        if (req.getAllowedIps() != null) key.setAllowedIps(req.getAllowedIps());
        if (req.getStatus() != null) key.setStatus(req.getStatus());
        if (req.getEnvironment() != null) key.setEnvironment(req.getEnvironment());
        if (req.getExpiredAt() != null) key.setExpiredAt(req.getExpiredAt());
        if (req.getRemark() != null) key.setRemark(req.getRemark());
        keyMapper.update(key);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        Map<String, Object> result = new HashMap<>();
        if (!List.of("active", "disabled").contains(status)) {
            result.put("success", false);
            result.put("message", "状态值无效");
            return result;
        }
        if (keyMapper.findById(id) == null) {
            result.put("success", false);
            result.put("message", "API Key 不存在");
            return result;
        }
        keyMapper.updateStatus(id, status);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> delete(Long id) {
        Map<String, Object> result = new HashMap<>();
        if (keyMapper.findById(id) == null) {
            result.put("success", false);
            result.put("message", "API Key 不存在");
            return result;
        }
        keyMapper.delete(id);
        result.put("success", true);
        return result;
    }

    /**
     * Verify an API Key is active and belongs to the given project.
     * Does NOT verify the HMAC signature — that is OpenApiAuthService's job.
     */
    @Cacheable(value = "apiKeys", key = "#apiKey + ':' + #projectId", unless = "#result == null")
    public ProjectApiKey findActiveKeyForProject(String apiKey, Long projectId) {
        ProjectApiKey key = keyMapper.findByApiKey(apiKey);
        if (key == null) return null;
        if (!"active".equals(key.getStatus())) return null;
        if (!projectId.equals(key.getProjectId())) return null;
        if (key.getExpiredAt() != null && key.getExpiredAt().isBefore(java.time.LocalDateTime.now())) return null;
        return key;
    }

    public boolean hasPermission(ProjectApiKey key, String permission) {
        if (key == null || key.getPermissions() == null) return false;
        try {
            @SuppressWarnings("unchecked")
            List<String> perms = objectMapper.readValue(key.getPermissions(), List.class);
            return perms.contains("*") || perms.contains(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildDefaultPermissions() {
        return "[\"cards:verify\",\"cards:consume\",\"cards:status\",\"cards:redeem\"]";
    }
}
