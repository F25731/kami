package org.xxg.backend.backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.mapper.ProjectMapper;

import java.security.SecureRandom;
import java.util.List;

@Service
public class ProjectService {
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final ProjectMapper projectMapper;
    private final SecureRandom random = new SecureRandom();

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    public List<Project> listProjects() {
        return projectMapper.findAll();
    }

    @Cacheable(value = "projects", key = "#id", unless = "#result == null")
    public Project getProject(Long id) {
        return projectMapper.findById(id);
    }

    @Cacheable(value = "projects", key = "#id", unless = "#result == null")
    public Project getById(Long id) {
        return projectMapper.findById(id);
    }

    @Cacheable(value = "projects", key = "'token:' + #token", unless = "#result == null")
    public Project getByToken(String token) {
        return projectMapper.findByToken(token);
    }

    public Project getDefaultProject() {
        return projectMapper.findDefaultProject();
    }

    @Transactional
    public Project createProject(Project project) {
        validateProject(project);
        if (projectMapper.existsByCode(project.getProjectCode())) {
            throw new RuntimeException("Project code already exists");
        }
        project.setProjectToken(generateUniqueToken());
        applyProjectDefaults(project);
        projectMapper.insert(project);
        return projectMapper.findByToken(project.getProjectToken());
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void updateProject(Long id, Project project) {
        Project existing = projectMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Project not found");
        }
        project.setId(id);
        if (project.getProjectCode() == null || project.getProjectCode().isBlank()) {
            project.setProjectCode(existing.getProjectCode());
        }
        applyProjectDefaults(project);
        projectMapper.update(project);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        projectMapper.updateStatus(id, status == null || status.isBlank() ? "enabled" : status);
    }

    @Transactional
    public String regenerateToken(Long id) {
        Project existing = projectMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Project not found");
        }
        String token = generateUniqueToken();
        projectMapper.updateToken(id, token);
        return token;
    }

    @Transactional
    public void deleteProject(Long id) {
        Project existing = projectMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Project not found");
        }
        if ("default".equals(existing.getProjectCode())) {
            throw new RuntimeException("Default project cannot be deleted");
        }
        projectMapper.delete(id);
    }

    public String buildBaseUrl(Project project, String origin) {
        String base = origin == null || origin.isBlank() ? "" : origin.replaceAll("/+$", "");
        return base + "/api/p/" + project.getProjectToken();
    }

    private void validateProject(Project project) {
        if (project == null || project.getProjectName() == null || project.getProjectName().isBlank()) {
            throw new RuntimeException("Project name is required");
        }
        if (project.getProjectCode() == null || project.getProjectCode().isBlank()) {
            throw new RuntimeException("Project code is required");
        }
    }

    private void applyProjectDefaults(Project p) {
        if (p.getProjectType() == null || p.getProjectType().isBlank()) p.setProjectType("other");
        if (p.getEnvironment() == null || p.getEnvironment().isBlank()) p.setEnvironment("production");
        if (p.getUsageMode() == null || p.getUsageMode().isBlank()) p.setUsageMode("direct_license");
        if (p.getStatus() == null || p.getStatus().isBlank()) p.setStatus("enabled");
        if (p.getDeviceBindMode() == null || p.getDeviceBindMode().isBlank()) {
            p.setDeviceBindMode(Boolean.TRUE.equals(p.getEnableDeviceBind()) ? defaultDeviceMode(p.getProjectType()) : "none");
        }
        if (p.getRateLimitPerMinute() == null) p.setRateLimitPerMinute(120);
        if (p.getMaxGeneratePerRequest() == null) p.setMaxGeneratePerRequest(100);
        if (p.getMaxGeneratePerDay() == null) p.setMaxGeneratePerDay(10000);
    }

    private String defaultDeviceMode(String projectType) {
        if ("windows".equals(projectType)) return "machine_code";
        if ("android".equals(projectType)) return "android_id";
        return "custom";
    }

    private String generateUniqueToken() {
        for (int attempts = 0; attempts < 50; attempts++) {
            StringBuilder sb = new StringBuilder(7);
            for (int i = 0; i < 7; i++) {
                sb.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
            }
            String token = sb.toString();
            if (!projectMapper.existsByToken(token)) {
                return token;
            }
        }
        throw new RuntimeException("Failed to generate project token");
    }
}
