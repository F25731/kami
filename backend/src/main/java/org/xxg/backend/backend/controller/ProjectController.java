package org.xxg.backend.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.service.ProjectService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/projects")
@CrossOrigin
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        return ResponseEntity.ok(Map.of("success", true, "data", projectService.listProjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        Project project = projectService.getProject(id);
        if (project == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Project not found"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("project", project);
        data.put("apiBaseUrl", projectService.buildBaseUrl(project, resolveOrigin(request)));
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Project project, HttpServletRequest request) {
        try {
            Project created = projectService.createProject(project);
            Map<String, Object> data = new HashMap<>();
            data.put("project", created);
            data.put("apiBaseUrl", projectService.buildBaseUrl(created, resolveOrigin(request)));
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody Project project) {
        try {
            projectService.updateProject(id, project);
            return ResponseEntity.ok(Map.of("success", true, "message", "Project updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            projectService.updateStatus(id, body.get("status"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Project status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/regenerate-token")
    public ResponseEntity<Map<String, Object>> regenerateToken(@PathVariable Long id, HttpServletRequest request) {
        try {
            String token = projectService.regenerateToken(id);
            Project project = projectService.getProject(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "project_token", token,
                    "apiBaseUrl", projectService.buildBaseUrl(project, resolveOrigin(request))
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Project deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private String resolveOrigin(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedProto != null && forwardedHost != null) {
            return forwardedProto + "://" + forwardedHost;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean standard = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + host + (standard ? "" : ":" + port);
    }
}
