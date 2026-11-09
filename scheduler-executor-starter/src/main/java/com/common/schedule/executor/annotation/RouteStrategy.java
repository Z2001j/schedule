package com.common.schedule.executor.annotation;

/**
 * 调度中心选择执行器实例的路由策略。
 */
public enum RouteStrategy {
    FIRST,
    LAST,
    ROUND,
    RANDOM,
    CONSISTENT_HASH,
    FAILOVER,
    SHARDING_BROADCAST
}