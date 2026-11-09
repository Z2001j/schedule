package com.common.schedule.admin.service;

/**
 * 业务校验/冲突异常，由全局异常处理器映射为 400。
 */
public class ScheduleException extends RuntimeException {

    public ScheduleException(String message) {
        super(message);
    }
}