package com.common.schedule.executor.client;

import com.common.schedule.executor.model.ApiResult;
import com.common.schedule.executor.model.CallbackRequest;
import com.common.schedule.executor.model.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 执行器 → 调度中心 HTTP 客户端。多地址依次尝试，失败不抛异常（避免拖垮执行器），
 * 由心跳周期重试。所有请求携带 X-Access-Token。
 */
public class AdminClient {

    private static final Logger log = LoggerFactory.getLogger(AdminClient.class);
    private static final String TOKEN_HEADER = "X-Access-Token";

    private final RestTemplate restTemplate;
    private final List<String> adminAddresses;
    private final String accessToken;

    public AdminClient(RestTemplate restTemplate, List<String> adminAddresses, String accessToken) {
        this.restTemplate = restTemplate;
        this.adminAddresses = adminAddresses;
        this.accessToken = accessToken;
    }

    public boolean register(RegisterRequest request) {
        return post("/api/registry/register", request);
    }

    public boolean beat(String appName, String address) {
        return post("/api/registry/beat", Map.of("appName", appName, "address", address));
    }

    public boolean remove(String appName, String address) {
        return post("/api/registry/remove", Map.of("appName", appName, "address", address));
    }

    public boolean callback(long logId, boolean success, long costMs, String errorMsg) {
        return post("/api/log/callback", new CallbackRequest(logId, success, costMs, errorMsg));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean post(String path, Object body) {
        if (adminAddresses == null || adminAddresses.isEmpty()) {
            log.warn("[scheduler-executor] 未配置调度中心地址 scheduler.executor.admin-addresses，跳过 {}", path);
            return false;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null && !accessToken.isEmpty()) {
            headers.set(TOKEN_HEADER, accessToken);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        for (String addr : adminAddresses) {
            String url = trimSlash(addr) + path;
            try {
                ResponseEntity<ApiResult> resp = restTemplate.exchange(url, HttpMethod.POST, entity, ApiResult.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && resp.getBody().isSuccess()) {
                    return true;
                }
                log.warn("[scheduler-executor] 调度中心返回失败 url={} body={}", url, resp.getBody());
            } catch (Exception e) {
                log.warn("[scheduler-executor] 调用调度中心失败 url={} : {}", url, e.getMessage());
            }
        }
        return false;
    }

    private static String trimSlash(String addr) {
        String a = addr.trim();
        return a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
    }
}