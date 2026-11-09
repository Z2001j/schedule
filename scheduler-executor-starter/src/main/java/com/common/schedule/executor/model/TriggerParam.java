package com.common.schedule.executor.model;

import com.common.schedule.executor.annotation.BlockStrategy;

/**
 * 调度中心下发给执行器的触发指令。
 */
public class TriggerParam {

    private String taskId;
    private long logId;
    private String params;
    private int shardIndex;
    private int shardTotal = 1;
    private BlockStrategy blockStrategy = BlockStrategy.SERIAL;
    /** SCHEDULE / MANUAL */
    private String triggerType = "SCHEDULE";

    public TriggerParam() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public void setShardIndex(int shardIndex) {
        this.shardIndex = shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setShardTotal(int shardTotal) {
        this.shardTotal = shardTotal;
    }

    public BlockStrategy getBlockStrategy() {
        return blockStrategy;
    }

    public void setBlockStrategy(BlockStrategy blockStrategy) {
        this.blockStrategy = blockStrategy;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
}