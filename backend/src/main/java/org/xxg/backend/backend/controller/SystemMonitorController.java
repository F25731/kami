package org.xxg.backend.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.service.SystemMonitorService;

import java.util.Map;

@RestController
@RequestMapping("/monitor")
public class SystemMonitorController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        try {
            return ResponseEntity.ok(systemMonitorService.getDatabaseStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            return ResponseEntity.ok(systemMonitorService.getSystemStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        try {
            return ResponseEntity.ok(systemMonitorService.getApiStatus());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        try {
            return ResponseEntity.ok(systemMonitorService.getOnlineUsers());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllMonitorData() {
        try {
            return ResponseEntity.ok(systemMonitorService.getAllMonitorData());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/check-update")
    public ResponseEntity<?> checkUpdate() {
        return ResponseEntity.ok(Map.of(
                "version", "license-center-local",
                "message", "本地私有化版本"
        ));
    }
}