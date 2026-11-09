package com.common.schedule.admin.web.dto;

import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.ScheduleType;

/**
 * 执行器注册时上报的任务元数据（注解默认值）。
 */
public class TaskMeta {

    private String taskId;
    private String name;
    private String description;
    private String group;
    private ScheduleType type;
    private String cron;
    private Long fixedRate;
    private Long fixedDelay;
    private Long initialDelay;
    private boolean enabled = true;
    private RouteStrategy route = RouteStrategy.FAILOVER;
    private BlockStrategy block = BlockStrategy.SERIAL;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ScheduleType getType() {
        return type;
    }

    public void setType(ScheduleType type) {
        this.type = type;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
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

    public RouteStrategy getRoute() {
        return route;
    }

    public void setRoute(RouteStrategy route) {
        this.route = route;
    }

    public BlockStrategy getBlock() {
        return block;
    }

    public void setBlock(BlockStrategy block) {
        this.block = block;
    }
}