package com.common.schedule.admin.client;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 调度中心 → 执行器 HTTP 客户端：下发触发指令与空闲探测。携带 X-Access-Token。
 */
@Component
public class ExecutorClient {

    private static final Logger log = LoggerFactory.getLogger(ExecutorClient.class);
    private static final String TOKEN_HEADER = "X-Access-Token";

    private final RestTemplate restTemplate;
    private final AdminProperties properties;

    public ExecutorClient(AdminProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTriggerTimeout());
        factory.setReadTimeout(properties.getTriggerTimeout());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 下发触发指令。返回是否被受理；网络/执行器异常返回 false（由调用方决定重试/告警）。
     */
    public boolean run(String address, TriggerParam param) {
        String url = trim(address) + "/scheduler-executor/run";
        try {
            ResponseEntity<ApiResult> resp = exchange(url, param);
            return resp.getBody() != null && resp.getBody().isSuccess();
        } catch (Exception e) {
            log.warn("[scheduler-admin] 触发执行器失败 url={} task={} : {}", url, param.getTaskId(), e.getMessage());
            return false;
        }
    }

    /**
     * 空闲探测（FAILOVER 路由用）。不可达或非空闲返回 false。
     */
    @SuppressWarnings("rawtypes")
    public boolean beat(String address) {
        String url = trim(address) + "/scheduler-executor/beat";
        try {
            ResponseEntity<ApiResult> resp = exchange(url, Map.of());
            ApiResult body = resp.getBody();
            return body != null && body.isSuccess() && Boolean.TRUE.equals(body.getData());
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<ApiResult> exchange(String url, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = properties.getAccessToken();
        if (token != null && !token.isEmpty()) {
            headers.set(TOKEN_HEADER, token);
        }
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), ApiResult.class);
    }

    private static String trim(String address) {
        String a = address.trim();
        return a.endsWith("/") ? a.substring(0, a.length() - 1) : a;
    }
}