package com.common.schedule.admin.config;

import com.common.schedule.admin.alarm.AlarmNotifier;
import com.common.schedule.admin.alarm.LogAlarmNotifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 调度中心基础设施 Bean。
 */
@Configuration
public class AdminBeanConfig {

    @Bean
    public ThreadPoolTaskScheduler adminTaskScheduler(AdminProperties properties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(properties.getPoolSize());
        scheduler.setThreadNamePrefix("admin-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(AlarmNotifier.class)
    public AlarmNotifier alarmNotifier() {
        return new LogAlarmNotifier();
    }
}