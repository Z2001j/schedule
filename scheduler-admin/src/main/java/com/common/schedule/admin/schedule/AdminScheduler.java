package com.common.schedule.admin.schedule;

import com.common.schedule.admin.ha.AdminLeaderElection;
import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态调度器：以 ThreadPoolTaskScheduler 承载每个任务的触发器，
 * 通过 ScheduledFuture 句柄实现运行中改周期/启停。DB 配置为权威来源。
 */
@Component
public class AdminScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdminScheduler.class);

    private final ThreadPoolTaskScheduler scheduler;
    private final TriggerService triggerService;
    private final AdminLeaderElection leaderElection;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    public AdminScheduler(ThreadPoolTaskScheduler adminTaskScheduler, TriggerService triggerService,
                          AdminLeaderElection leaderElection) {
        this.scheduler = adminTaskScheduler;
        this.triggerService = triggerService;
        this.leaderElection = leaderElection;
    }

    /**
     * 按配置登记触发器；已存在则先取消。配置非法时记录告警并跳过，不抛出以免拖垮批量加载。
     */
    public synchronized void schedule(TaskConfig config) {
        stop(config.getTaskId());
        if (!config.isEnabled()) {
            return;
        }
        Runnable job = () -> runScheduled(config.getTaskId());
        try {
            ScheduledFuture<?> future = buildFuture(config, job);
            if (future != null) {
                futures.put(config.getTaskId(), future);
            }
        } catch (Exception e) {
            log.error("schedule task failed: {} : {}", config.getTaskId(), e.getMessage());
        }
    }

    /**
     * 到点触发入口：HA 模式下仅 leader 真正下发，follower 空跑 standby。
     * 手动触发不经此路径，任意节点均可执行。
     */
    void runScheduled(String taskId) {
        if (!leaderElection.isLeader()) {
            return;
        }
        triggerService.trigger(taskId, TriggerService.TYPE_SCHEDULE);
    }

    private ScheduledFuture<?> buildFuture(TaskConfig config, Runnable job) {
        long initialDelay = config.getInitialDelay() == null ? 0L : config.getInitialDelay();
        Instant start = Instant.now().plusMillis(Math.max(0, initialDelay));
        ScheduleType type = config.getScheduleType() == null ? ScheduleType.CRON : config.getScheduleType();
        switch (type) {
            case CRON:
                if (config.getCronExpression() == null || config.getCronExpression().isBlank()) {
                    log.warn("task {} has no cron expression, skip", config.getTaskId());
                    return null;
                }
                return scheduler.schedule(job, new CronTrigger(config.getCronExpression()));
            case FIXED_RATE:
                if (!positive(config.getFixedRate())) {
                    log.warn("task {} fixedRate invalid, skip", config.getTaskId());
                    return null;
                }
                return scheduler.scheduleAtFixedRate(job, start, Duration.ofMillis(config.getFixedRate()));
            case FIXED_DELAY:
                if (!positive(config.getFixedDelay())) {
                    log.warn("task {} fixedDelay invalid, skip", config.getTaskId());
                    return null;
                }
                return scheduler.scheduleWithFixedDelay(job, start, Duration.ofMillis(config.getFixedDelay()));
            default:
                return null;
        }
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    public synchronized void stop(String taskId) {
        ScheduledFuture<?> future = futures.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 重新加载配置：启用则重建触发器，停用则取消。
     */
    public synchronized void reschedule(TaskConfig config) {
        if (config.isEnabled()) {
            schedule(config);
        } else {
            stop(config.getTaskId());
        }
    }

    public boolean isScheduled(String taskId) {
        ScheduledFuture<?> future = futures.get(taskId);
        return future != null && !future.isCancelled();
    }

    @PreDestroy
    public void destroy() {
        futures.values().forEach(f -> f.cancel(false));
        futures.clear();
    }
}