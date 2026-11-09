package com.common.schedule.admin.model;

import java.time.LocalDateTime;

/**
 * 控制台任务视图：配置 + 运行时信息 + 下次执行时间 + 在线实例数。
 */
public class TaskView {

    private String taskId;
    private String appName;
    private String taskName;
    private String taskGroup;
    private String description;
    private ScheduleType scheduleType;
    private String cronExpression;
    private Long fixedRate;
    private Long fixedDelay;
    private Long initialDelay;
    private boolean enabled;
    private RouteStrategy routeStrategy;
    private BlockStrategy blockStrategy;
    private int retryCount;
    private int version;
    private LocalDateTime lastExecTime;
    private Boolean lastExecSuccess;
    private String lastError;
    private LocalDateTime nextExecTime;
    private int onlineInstances;

    public static TaskView from(TaskConfig c) {
        TaskView v = new TaskView();
        v.taskId = c.getTaskId();
        v.appName = c.getAppName();
        v.taskName = c.getTaskName();
        v.taskGroup = c.getTaskGroup();
        v.description = c.getDescription();
        v.scheduleType = c.getScheduleType();
        v.cronExpression = c.getCronExpression();
        v.fixedRate = c.getFixedRate();
        v.fixedDelay = c.getFixedDelay();
        v.initialDelay = c.getInitialDelay();
        v.enabled = c.isEnabled();
        v.routeStrategy = c.getRouteStrategy();
        v.blockStrategy = c.getBlockStrategy();
        v.retryCount = c.getRetryCount();
        v.version = c.getVersion();
        return v;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskGroup() {
        return taskGroup;
    }

    public void setTaskGroup(String taskGroup) {
        this.taskGroup = taskGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Long getFixedRate() {
        return fixedRate;
    }

    public void setFixedRate(Long fixedRate) {
        this.fixedRate = fixedRate;
    }

    public Long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(Long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }

    public Long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RouteStrategy getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public BlockStrategy getBlockStrategy() {
        return blockStrategy;
    }

    public void setBlockStrategy(BlockStrategy blockStrategy) {
        this.blockStrategy = blockStrategy;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getLastExecTime() {
        return lastExecTime;
    }

    public void setLastExecTime(LocalDateTime lastExecTime) {
        this.lastExecTime = lastExecTime;
    }

    public Boolean getLastExecSuccess() {
        return lastExecSuccess;
    }

    public void setLastExecSuccess(Boolean lastExecSuccess) {
        this.lastExecSuccess = lastExecSuccess;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getNextExecTime() {
        return nextExecTime;
    }

    public void setNextExecTime(LocalDateTime nextExecTime) {
        this.nextExecTime = nextExecTime;
    }

    public int getOnlineInstances() {
        return onlineInstances;
    }

    public void setOnlineInstances(int onlineInstances) {
        this.onlineInstances = onlineInstances;
    }
}