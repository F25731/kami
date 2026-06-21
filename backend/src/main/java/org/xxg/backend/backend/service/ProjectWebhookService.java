package org.xxg.backend.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Map;

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

    @Async
    public void trigger(Long projectId, String event, Map<String, Object> data) {
        try {
            Project project = projectMapper.findById(projectId);
            if (!canSend(project, event, false)) {
                return;
            }
            send(project, event, data == null ? Map.of() : data);
            System.out.println("[WebHook] Triggered: project=" + projectId + ", event=" + event);
        } catch (Exception e) {
            System.err.println("[WebHook] Failed: project=" + projectId + ", event=" + event + ", error=" + e.getMessage());
        }
    }

    public void test(Long projectId) {
        Project project = projectMapper.findById(projectId);
        if (!canSend(project, "webhook.test", true)) {
            throw new IllegalArgumentException("WebHook is not enabled or URL is empty");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("message", "WebHook test from License Center");
        data.put("sent_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        send(project, "webhook.test", data);
    }

    private boolean canSend(Project project, String event, boolean bypassEventFilter) {
        if (project == null || !Boolean.TRUE.equals(project.getWebhookEnabled())) {
            return false;
        }
        if (project.getWebhookUrl() == null || project.getWebhookUrl().isBlank()) {
            return false;
        }
        return bypassEventFilter || eventAllowed(project.getWebhookEvents(), event);
    }

    private boolean eventAllowed(String configuredEvents, String event) {
        if (configuredEvents == null || configuredEvents.isBlank()) {
            return true;
        }
        try {
            List<String> events = objectMapper.readValue(configuredEvents, new TypeReference<>() {});
            return events.isEmpty() || events.contains(event);
        } catch (Exception ignored) {
            for (String item : configuredEvents.split(",")) {
                if (event.equals(item.trim())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void send(Project project, String event, Map<String, Object> data) {
        try {
            Map<String, Object> payload = buildPayload(project, event, data);
            String payloadJson = objectMapper.writeValueAsString(payload);

            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            httpHeaders.add("X-Webhook-Event", event);
            httpHeaders.add("X-Project-Token", project.getProjectToken());
            httpHeaders.add("X-Timestamp", String.valueOf(System.currentTimeMillis() / 1000));

            if (project.getWebhookSecret() != null && !project.getWebhookSecret().isBlank()) {
                String signature = SignatureUtil.sha256(payloadJson + project.getWebhookSecret());
                httpHeaders.add("X-Webhook-Signature", signature);
            }

            org.springframework.http.HttpEntity<String> request = new org.springframework.http.HttpEntity<>(payloadJson, httpHeaders);
            restTemplate.postForEntity(project.getWebhookUrl(), request, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Map<String, Object> buildPayload(Project project, String event, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("project_id", project.getId());
        payload.put("project_code", project.getProjectCode());
        payload.put("project_token", project.getProjectToken());
        payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("data", data);
        return payload;
    }
}
