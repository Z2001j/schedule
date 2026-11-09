package com.common.schedule.executor.model;

/**
 * 执行结果回调请求。
 */
public class CallbackRequest {

    private long logId;
    private boolean success;
    private long costMs;
    private String errorMsg;

    public CallbackRequest() {
    }

    public CallbackRequest(long logId, boolean success, long costMs, String errorMsg) {
        this.logId = logId;
        this.success = success;
        this.costMs = costMs;
        this.errorMsg = errorMsg;
    }

    public long getLogId() {
        return logId;
    }

    public void setLogId(long logId) {
        this.logId = logId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getCostMs() {
        return costMs;
    }

    public void setCostMs(long costMs) {
        this.costMs = costMs;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}