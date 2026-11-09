package com.common.schedule.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调度中心配置，前缀 scheduler.admin。
 */
@ConfigurationProperties(prefix = "scheduler.admin")
public class AdminProperties {

    private boolean enabled = true;
    private int poolSize = 8;
    private String tableName = "sched_task";
    private String executorTableName = "sched_executor";
    private String logTableName = "sched_task_log";
    private boolean autoDdl = true;
    private String accessToken = "";
    /** 心跳超时（秒），超过则剔除下线 */
    private int heartbeatTimeout = 90;
    /** 下线剔除扫描周期（秒） */
    private int reaperInterval = 30;
    /** HTTP 触发超时（毫秒） */
    private int triggerTimeout = 5000;
    /** 固定频率/延迟最小值（毫秒） */
    private long minPeriodMs = 1000;
    /** 是否启用调度中心 HA（多 admin 选主）。单实例部署保持 false */
    private boolean haEnabled = false;
    /** 选主/续约周期（秒） */
    private int electionInterval = 10;
    /** leader 心跳超时（秒），超过则允许 follower 接管 */
    private int leaderTimeout = 30;
    /** 选主锁表名 */
    private String leaderTableName = "sched_leader";
    /** 是否对控制台管理 API（/api/tasks、/api/executors）启用令牌鉴权。生产应保持 true */
    private boolean managementAuthEnabled = true;
    /** 管理 API 专用令牌；为空则回退使用 accessToken。与执行器 accessToken 分离可实现权限隔离 */
    private String managementToken = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getExecutorTableName() {
        return executorTableName;
    }

    public void setExecutorTableName(String executorTableName) {
        this.executorTableName = executorTableName;
    }

    public String getLogTableName() {
        return logTableName;
    }

    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }

    public boolean isAutoDdl() {
        return autoDdl;
    }

    public void setAutoDdl(boolean autoDdl) {
        this.autoDdl = autoDdl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public int getReaperInterval() {
        return reaperInterval;
    }

    public void setReaperInterval(int reaperInterval) {
        this.reaperInterval = reaperInterval;
    }

    public int getTriggerTimeout() {
        return triggerTimeout;
    }

    public void setTriggerTimeout(int triggerTimeout) {
        this.triggerTimeout = triggerTimeout;
    }

    public long getMinPeriodMs() {
        return minPeriodMs;
    }

    public void setMinPeriodMs(long minPeriodMs) {
        this.minPeriodMs = minPeriodMs;
    }

    public boolean isHaEnabled() {
        return haEnabled;
    }

    public void setHaEnabled(boolean haEnabled) {
        this.haEnabled = haEnabled;
    }

    public int getElectionInterval() {
        return electionInterval;
    }

    public void setElectionInterval(int electionInterval) {
        this.electionInterval = electionInterval;
    }

    public int getLeaderTimeout() {
        return leaderTimeout;
    }

    public void setLeaderTimeout(int leaderTimeout) {
        this.leaderTimeout = leaderTimeout;
    }

    public String getLeaderTableName() {
        return leaderTableName;
    }

    public void setLeaderTableName(String leaderTableName) {
        this.leaderTableName = leaderTableName;
    }

    public boolean isManagementAuthEnabled() {
        return managementAuthEnabled;
    }

    public void setManagementAuthEnabled(boolean managementAuthEnabled) {
        this.managementAuthEnabled = managementAuthEnabled;
    }

    public String getManagementToken() {
        return managementToken;
    }

    public void setManagementToken(String managementToken) {
        this.managementToken = managementToken;
    }
}