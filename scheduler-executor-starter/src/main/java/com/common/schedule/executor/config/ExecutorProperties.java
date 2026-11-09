package com.common.schedule.executor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行器配置，前缀 scheduler.executor。
 */
@ConfigurationProperties(prefix = "scheduler.executor")
public class ExecutorProperties {

    /** 总开关 */
    private boolean enabled = true;

    /** 业务服务唯一标识 */
    private String appName = "application";

    /** 调度中心地址列表，支持逗号分隔，如 http://admin:8080 */
    private List<String> adminAddresses = new ArrayList<>();

    /** 执行器端点端口 */
    private int port = 9999;

    /** 与调度中心共享的访问令牌 */
    private String accessToken = "";

    /** 心跳间隔（秒） */
    private int heartbeatInterval = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getAdminAddresses() {
        return adminAddresses;
    }

    public void setAdminAddresses(List<String> adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}