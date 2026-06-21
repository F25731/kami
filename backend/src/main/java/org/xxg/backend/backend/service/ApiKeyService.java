package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.entity.User;
import org.xxg.backend.backend.mapper.ApiKeyMapper;
import org.xxg.backend.backend.mapper.ProjectMapper;
import org.xxg.backend.backend.util.PasswordUtil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {
    private final ApiKeyMapper apiKeyMapper;
    private final ProjectMapper projectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyMapper apiKeyMapper, ProjectMapper projectMapper) {
        this.apiKeyMapper = apiKeyMapper;
        this.projectMapper = projectMapper;
    }

    public List<ApiKey> getAllApiKeys() {
        List<ApiKey> keys = apiKeyMapper.findAll();
        enrich(keys);
        return keys;
    }

    public List<ApiKey> getProjectApiKeys(Long projectId) {
        List<ApiKey> keys = apiKeyMapper.findByProjectId(projectId);
        enrich(keys);
        return keys;
    }

    public ApiKey getByApiKey(String apiKey) {
        return apiKeyMapper.findByApiKey(apiKey);
    }

    public ApiKey getByProjectAndApiKey(Long projectId, String apiKey) {
        return apiKeyMapper.findByProjectAndApiKey(projectId, apiKey);
    }

    @Transactional
    public void updateUsage(Long id) {
        apiKeyMapper.updateUsage(id);
    }

    @Transactional
    public ApiKey createApiKey(String name, String description, Boolean enableCardEncryption) {
        Project defaultProject = projectMapper.findDefaultProject();
        Long projectId = defaultProject != null ? defaultProject.getId() : null;
        return createApiKey(projectId, name, description, enableCardEncryption, null, null, null, null);
    }

    @Transactional
    public ApiKey createApiKey(Long projectId, String name, String description, Boolean enableCardEncryption,
                              String permissions, String allowedIps, String environment, LocalDateTime expiredAt) {
        ApiKey apiKey = new ApiKey();
        apiKey.setProjectId(projectId);
        apiKey.setKeyName(name == null || name.isBlank() ? "API Key" : name);
        apiKey.setDescription(description);
        apiKey.setEnableCardEncryption(enableCardEncryption);
        apiKey.setApiKey("ak_" + UUID.randomUUID().toString().replace("-", ""));
        String secret = generateSecret();
        apiKey.setApiSecret(secret);
        apiKey.setApiSecretHash(PasswordUtil.hashPassword(secret));
        apiKey.setPermissions(permissions == null || permissions.isBlank() ? defaultPermissions() : permissions);
        apiKey.setAllowedIps(allowedIps);
        apiKey.setEnvironment(environment == null || environment.isBlank() ? "production" : environment);
        apiKey.setExpiredAt(expiredAt);
        apiKey.setKeyValue(UUID.randomUUID().toString());
        apiKey.setName(apiKey.getKeyName());
        apiKey.setStatus(1);
        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    @Transactional
    public void updateApiKey(Long id, String name, String description, Integer status, String webhookConfig, Boolean enableCardEncryption) {
        updateApiKey(id, name, description, status, webhookConfig, enableCardEncryption, null, null, false, false);
    }

    @Transactional
    public void updateApiKey(Long id, String name, String description, Integer status, String webhookConfig,
                            Boolean enableCardEncryption, Boolean requireMachineCode, String machineSpecOnceConfig,
                            boolean updateSpecConfig, boolean updateWebhookConfig) {
        updateApiKey(id, name, description, status, webhookConfig, enableCardEncryption, requireMachineCode,
                machineSpecOnceConfig, updateSpecConfig, updateWebhookConfig, null, null, null, null);
    }

    @Transactional
    public void updateApiKey(Long id, String name, String description, Integer status, String webhookConfig,
                            Boolean enableCardEncryption, Boolean requireMachineCode, String machineSpecOnceConfig,
                            boolean updateSpecConfig, boolean updateWebhookConfig, String permissions,
                            String allowedIps, String environment, LocalDateTime expiredAt) {
        ApiKey apiKey = apiKeyMapper.findById(id);
        if (apiKey == null) {
            return;
        }
        apiKey.setKeyName(name);
        apiKey.setName(name);
        apiKey.setDescription(description);
        apiKey.setStatus(status == null ? 1 : status);
        if (updateWebhookConfig) apiKey.setWebhookConfig(webhookConfig);
        apiKey.setEnableCardEncryption(enableCardEncryption);
        if (requireMachineCode != null) apiKey.setRequireMachineCode(requireMachineCode);
        if (updateSpecConfig) apiKey.setMachineSpecOnceConfig(machineSpecOnceConfig);
        if (permissions != null) apiKey.setPermissions(permissions);
        if (allowedIps != null) apiKey.setAllowedIps(allowedIps);
        if (environment != null) apiKey.setEnvironment(environment);
        if (expiredAt != null) apiKey.setExpiredAt(expiredAt);
        apiKeyMapper.update(apiKey);
    }

    @Transactional
    public String resetSecret(Long id) {
        String secret = generateSecret();
        apiKeyMapper.updateSecretHash(id, PasswordUtil.hashPassword(secret));
        return secret;
    }

    @Transactional
    public void deleteApiKey(Long id) {
        apiKeyMapper.delete(id);
    }

    @Transactional
    public void assignUser(Long apiKeyId, Long userId) {
        List<User> users = apiKeyMapper.getAssignedUsers(apiKeyId);
        boolean exists = users.stream().anyMatch(u -> u.getId().equals(userId));
        if (!exists) {
            apiKeyMapper.assignUser(apiKeyId, userId);
        }
    }

    @Transactional
    public void unassignUser(Long apiKeyId, Long userId) {
        apiKeyMapper.unassignUser(apiKeyId, userId);
    }

    public boolean hasPermission(ApiKey apiKey, String permission) {
        if (apiKey == null || permission == null) return false;
        String permissions = apiKey.getPermissions();
        return permissions == null || permissions.isBlank() || permissions.contains("*") || permissions.contains(permission);
    }

    private void enrich(List<ApiKey> keys) {
        for (ApiKey key : keys) {
            List<User> users = apiKeyMapper.getAssignedUsers(key.getId());
            key.setAssignedUsers(users);
            key.setUserCount(users.size());
            key.setCardCount(0);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String defaultPermissions() {
        return "card:generate,card:verify,card:consume,card:status,card:redeem,card:unbind,card:refund,entitlement:status,entitlement:consume,entitlement:refund";
    }
}
