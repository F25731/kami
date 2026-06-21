package org.xxg.backend.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xxg.backend.backend.entity.Project;
import org.xxg.backend.backend.mapper.ProjectMapper;
import org.xxg.backend.backend.util.SignatureUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目级 WebHook 服务
 * 支持事件：card.generated, card.consumed, card.redeemed, entitlement.consumed, device.bound, device.unbound
 */
@Service
public class ProjectWebhookService {

    private final ProjectMapper projectMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ProjectWebhookService(ProjectMapper projectMapper, ObjectMapper objectMapper) {
        this.projectMapper = projectMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 异步触发 WebHook
     */
    @Async
    public void trigger(Long projectId, String event, Map<String, Object> data) {
        try {
            Project project = projectMapper.findById(projectId);
            if (project == null || !Boolean.TRUE.equals(project.getWebhookEnabled())) {
                return;
            }
            if (project.getWebhookUrl() == null || project.getWebhookUrl().isBlank()) {
                return;
            }

            Map<String, Object> payload = buildPayload(project, event, data);
            String payloadJson = objectMapper.writeValueAsString(payload);

            // 如果配置了签名密钥，添加签名
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Webhook-Event", event);
            headers.put("X-Project-Token", project.getProjectToken());
            headers.put("X-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));

            if (project.getWebhookSecret() != null && !project.getWebhookSecret().isBlank()) {
                String signature = SignatureUtil.sha256(payloadJson + project.getWebhookSecret());
                headers.put("X-Webhook-Signature", signature);
            }

            // 发送 HTTP POST
            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            headers.forEach(httpHeaders::add);
            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(payloadJson, httpHeaders);

            restTemplate.postForEntity(project.getWebhookUrl(), request, String.class);

            System.out.println("[WebHook] Triggered: project=" + projectId + ", event=" + event);
        } catch (Exception e) {
            System.err.println("[WebHook] Failed: project=" + projectId + ", event=" + event + ", error=" + e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(Project project, String event, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("project_id", project.getId());
        payload.put("project_code", project.getProjectCode());
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("data", data);
        return payload;
    }
}
