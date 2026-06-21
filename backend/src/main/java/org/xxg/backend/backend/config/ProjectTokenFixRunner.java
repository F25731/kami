package org.xxg.backend.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.service.ProjectService;

/**
 * 启动时自动修复 default 项目的 project_token（如果是 'DEFAULT' 字符串则改为7位随机码）
 */
@Component
public class ProjectTokenFixRunner implements CommandLineRunner {

    private final ProjectService projectService;

    public ProjectTokenFixRunner(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public void run(String... args) {
        try {
            Project defaultProject = projectService.getDefaultProject();
            if (defaultProject == null) {
                System.out.println("[ProjectTokenFix] No default project found, skipping token fix.");
                return;
            }

            String token = defaultProject.getProjectToken();
            if (token == null || token.equals("DEFAULT") || token.length() != 7) {
                System.out.println("[ProjectTokenFix] Default project token is invalid: " + token + ", regenerating...");
                String newToken = projectService.regenerateToken(defaultProject.getId());
                System.out.println("[ProjectTokenFix] Default project token fixed: " + newToken);
            } else {
                System.out.println("[ProjectTokenFix] Default project token is valid: " + token);
            }
        } catch (Exception e) {
            System.err.println("[ProjectTokenFix] Failed to fix default project token: " + e.getMessage());
        }
    }
}
