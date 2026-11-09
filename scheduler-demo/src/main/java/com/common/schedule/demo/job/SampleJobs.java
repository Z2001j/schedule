package com.common.schedule.demo.job;

import com.common.schedule.executor.annotation.BlockStrategy;
import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.annotation.RouteStrategy;
import com.common.schedule.executor.annotation.ScheduleType;
import com.common.schedule.executor.execute.ShardingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 示例任务：加注解即纳管。周期/启停/路由仅为首次注册默认值，运行期以调度中心为准。
 */
@Component
public class SampleJobs {

    private static final Logger log = LoggerFactory.getLogger(SampleJobs.class);

    private final AtomicInteger counter = new AtomicInteger();

    @ManagedScheduled(name = "问候任务", description = "每 10 秒打印一次问候",
            type = ScheduleType.CRON, cron = "0/10 * * * * ?", enabled = true)
    public void helloJob() throws InterruptedException {
        int n = counter.incrementAndGet();
        Thread.sleep(120);
        log.info("[demo] helloJob 执行第 {} 次", n);
    }

    @ManagedScheduled(name = "分片任务", description = "分片广播示例，按实例分片",
            type = ScheduleType.CRON, cron = "0 0/1 * * * ?", enabled = false,
            route = RouteStrategy.SHARDING_BROADCAST)
    public void shardingJob() {
        log.info("[demo] shardingJob 执行，分片 {}/{}", ShardingContext.getShardIndex(), ShardingContext.getShardTotal());
    }

    @ManagedScheduled(name = "固定频率任务", description = "每 3 秒固定频率执行",
            type = ScheduleType.FIXED_RATE, fixedRate = 3000, enabled = false,
            block = BlockStrategy.DISCARD)
    public void fixedRateJob() {
        log.info("[demo] fixedRateJob 执行");
    }
}