package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.entity.ProjectApiKey;
import org.xxg.backend.backend.service.ProjectApiKeyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/projects/{projectId}/api-keys")
public class ProjectApiKeyController {

    private final ProjectApiKeyService apiKeyService;

    public ProjectApiKeyController(ProjectApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listKeys(@PathVariable Long projectId) {
        List<ProjectApiKey> keys = apiKeyService.listByProjectId(projectId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", keys);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createKey(@PathVariable Long projectId, @RequestBody ProjectApiKey req) {
        Map<String, Object> result = apiKeyService.create(projectId, req);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateKey(@PathVariable Long projectId, @PathVariable Long id, @RequestBody ProjectApiKey req) {
        Map<String, Object> result = apiKeyService.update(id, req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<Map<String, Object>> rotateKey(@PathVariable Long projectId, @PathVariable Long id) {
        Map<String, Object> result = apiKeyService.rotate(id);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long projectId, @PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Map<String, Object> result = apiKeyService.updateStatus(id, status);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteKey(@PathVariable Long projectId, @PathVariable Long id) {
        Map<String, Object> result = apiKeyService.delete(id);
        return ResponseEntity.ok(result);
    }
}
