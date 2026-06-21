package org.xxg.backend.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.mapper.UserMapper;
import org.xxg.backend.backend.service.ApiKeyService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin
public class ApiKeyController {
    private final ApiKeyService apiKeyService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyController(ApiKeyService apiKeyService, UserMapper userMapper) {
        this.apiKeyService = apiKeyService;
        this.userMapper = userMapper;
    }

    @GetMapping("/apikeys")
    public ResponseEntity<?> getAllApiKeys() {
        try {
            return ResponseEntity.ok(apiKeyService.getAllApiKeys());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getClass().getName(), "message", e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}/apikeys")
    public ResponseEntity<?> getProjectApiKeys(@PathVariable Long projectId) {
        return ResponseEntity.ok(Map.of("success", true, "data", apiKeyService.getProjectApiKeys(projectId)));
    }

    @PostMapping("/apikeys")
    public ResponseEntity<ApiKey> createApiKey(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(createApiKeyInternal(null, body));
    }

    @PostMapping("/projects/{projectId}/apikeys")
    public ResponseEntity<Map<String, Object>> createProjectApiKey(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        ApiKey created = createApiKeyInternal(projectId, body);
        return ResponseEntity.ok(Map.of("success", true, "data", created, "api_secret", created.getApiSecret()));
    }

    @PutMapping("/apikeys/{id}")
    public ResponseEntity<Void> updateApiKey(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        updateApiKeyInternal(id, body);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/projects/{projectId}/apikeys/{id}")
    public ResponseEntity<Map<String, Object>> updateProjectApiKey(@PathVariable Long projectId, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        updateApiKeyInternal(id, body);
        return ResponseEntity.ok(Map.of("success", true, "message", "API Key updated"));
    }

    @PostMapping("/apikeys/{id}/reset-secret")
    public ResponseEntity<Map<String, Object>> resetSecret(@PathVariable Long id) {
        String secret = apiKeyService.resetSecret(id);
        return ResponseEntity.ok(Map.of("success", true, "api_secret", secret));
    }

    @DeleteMapping("/apikeys/{id}")
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/projects/{projectId}/apikeys/{id}")
    public ResponseEntity<Map<String, Object>> deleteProjectApiKey(@PathVariable Long projectId, @PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "API Key deleted"));
    }

    @PostMapping("/apikeys/{id}/users")
    public ResponseEntity<Void> assignUser(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        apiKeyService.assignUser(id, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/apikeys/{id}/users/{userId}")
    public ResponseEntity<Void> unassignUser(@PathVariable Long id, @PathVariable Long userId) {
        apiKeyService.unassignUser(id, userId);
        return ResponseEntity.ok().build();
    }

    private ApiKey createApiKeyInternal(Long pathProjectId, Map<String, Object> body) {
        Long projectId = pathProjectId;
        if (projectId == null && body.get("project_id") != null) {
            projectId = Long.valueOf(body.get("project_id").toString());
        }
        String name = str(body.get("name"));
        String description = str(body.get("description"));
        Boolean enableCardEncryption = bool(body.get("enable_card_encryption"));
        String permissions = normalizeJsonOrString(body.get("permissions"));
        String allowedIps = normalizeJsonOrString(body.get("allowed_ips"));
        String environment = str(body.get("environment"));
        LocalDateTime expiredAt = parseTime(body.get("expired_at"));
        return apiKeyService.createApiKey(projectId, name, description, enableCardEncryption, permissions, allowedIps, environment, expiredAt);
    }

    private void updateApiKeyInternal(Long id, Map<String, Object> body) {
        String name = str(body.get("name"));
        String description = str(body.get("description"));
        Integer status = body.containsKey("status") ? Integer.valueOf(body.get("status").toString()) : 1;
        Boolean enableCardEncryption = body.containsKey("enable_card_encryption") ? bool(body.get("enable_card_encryption")) : false;
        Boolean requireMachineCode = body.containsKey("require_machine_code") ? bool(body.get("require_machine_code")) : null;
        boolean updateSpecConfig = body.containsKey("machine_spec_once_config");
        String machineSpecOnceConfig = updateSpecConfig ? normalizeJsonOrString(body.get("machine_spec_once_config")) : null;
        boolean updateWebhook = body.containsKey("webhook_config");
        String webhookStr = updateWebhook ? normalizeJsonOrString(body.get("webhook_config")) : null;
        apiKeyService.updateApiKey(id, name, description, status, webhookStr, enableCardEncryption, requireMachineCode,
                machineSpecOnceConfig, updateSpecConfig, updateWebhook, normalizeJsonOrString(body.get("permissions")),
                normalizeJsonOrString(body.get("allowed_ips")), str(body.get("environment")), parseTime(body.get("expired_at")));
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private Boolean bool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDateTime parseTime(Object value) {
        if (value == null || value.toString().isBlank()) return null;
        return LocalDateTime.parse(value.toString().replace(" ", "T"));
    }

    private String normalizeJsonOrString(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
