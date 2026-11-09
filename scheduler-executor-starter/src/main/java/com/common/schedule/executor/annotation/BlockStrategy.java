package com.common.schedule.executor.annotation;

/**
 * 任务上次未结束时再次触发的阻塞处理策略。
 */
public enum BlockStrategy {
    /** 排队串行执行 */
    SERIAL,
    /** 丢弃本次触发 */
    DISCARD,
    /** 清空待执行队列，执行最新一次 */
    OVERRIDE
}