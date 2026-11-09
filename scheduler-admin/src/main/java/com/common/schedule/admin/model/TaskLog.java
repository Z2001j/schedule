package com.common.schedule.admin.model;

import java.time.LocalDateTime;

/**
 * 触发/执行日志。触发时由调度中心写入，执行结果由执行器回调后更新。
 */
public class TaskLog {

    private long id;
    private String taskId;
    private LocalDateTime triggerTime;
    private String triggerType;
    private RouteStrategy routeStrategy;
    private String executorAddress;
    private Integer shardIndex;
    private Integer shardTotal;
    /** TRIGGERED / FAIL */
    private String triggerResult;
    private LocalDateTime execStart;
    private LocalDateTime execEnd;
    private Long costMs;
    private Boolean success;
    private String errorMsg;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public RouteStrategy getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public String getExecutorAddress() {
        return executorAddress;
    }

    public void setExecutorAddress(String executorAddress) {
        this.executorAddress = executorAddress;
    }

    public Integer getShardIndex() {
        return shardIndex;
    }

    public void setShardIndex(Integer shardIndex) {
        this.shardIndex = shardIndex;
    }

    public Integer getShardTotal() {
        return shardTotal;
    }

    public void setShardTotal(Integer shardTotal) {
        this.shardTotal = shardTotal;
    }

    public String getTriggerResult() {
        return triggerResult;
    }

    public void setTriggerResult(String triggerResult) {
        this.triggerResult = triggerResult;
    }

    public LocalDateTime getExecStart() {
        return execStart;
    }

    public void setExecStart(LocalDateTime execStart) {
        this.execStart = execStart;
    }

    public LocalDateTime getExecEnd() {
        return execEnd;
    }

    public void setExecEnd(LocalDateTime execEnd) {
        this.execEnd = execEnd;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}