package org.xxg.backend.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.ApiCallLog;
import org.xxg.backend.backend.mapper.ApiCallLogMapper;

import java.util.List;

@Service
public class ApiCallLogService {

    private final ApiCallLogMapper logMapper;

    public ApiCallLogService(ApiCallLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Async
    public void logAsync(Long projectId, Long apiKeyId, String apiKey, String endpoint,
                          String method, String requestBody, int responseCode, String responseBody,
                          String clientIp, long durationMs, String status, String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setProjectId(projectId);
        log.setApiKeyId(apiKeyId);
        log.setApiKey(apiKey);
        log.setEndpoint(endpoint);
        log.setMethod(method);
        log.setRequestBody(requestBody);
        log.setResponseCode(responseCode);
        log.setResponseBody(responseBody);
        log.setClientIp(clientIp);
        log.setDurationMs(durationMs);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        try {
            logMapper.insert(log);
        } catch (Exception ignored) {
            // log failure must never affect the caller
        }
    }

    public List<ApiCallLog> listByProjectId(Long projectId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return logMapper.findByProjectId(projectId, offset, pageSize);
    }

    public int countByProjectId(Long projectId) {
        return logMapper.countByProjectId(projectId);
    }
}
