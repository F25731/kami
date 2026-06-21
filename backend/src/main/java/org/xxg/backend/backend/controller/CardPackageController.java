package org.xxg.backend.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.entity.CardPackage;
import org.xxg.backend.backend.service.CardPackageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/packages")
public class CardPackageController {

    private final CardPackageService packageService;

    public CardPackageController(CardPackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listPackages(@RequestParam(required = false) Long projectId) {
        List<CardPackage> packages = projectId != null
            ? packageService.listByProjectId(projectId)
            : packageService.listByProjectId(1L);  // default project
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", packages);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPackage(@PathVariable Long id) {
        CardPackage pkg = packageService.getById(id);
        Map<String, Object> result = new HashMap<>();
        if (pkg == null) {
            result.put("success", false);
            result.put("message", "套餐不存在");
            return ResponseEntity.status(404).body(result);
        }
        result.put("success", true);
        result.put("data", pkg);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPackage(@RequestBody CardPackage pkg) {
        Map<String, Object> result = packageService.create(pkg);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePackage(@PathVariable Long id, @RequestBody CardPackage pkg) {
        Map<String, Object> result = packageService.update(id, pkg);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        Map<String, Object> result = packageService.updateStatus(id, status);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePackage(@PathVariable Long id) {
        Map<String, Object> result = packageService.delete(id);
        return ResponseEntity.ok(result);
    }
}
