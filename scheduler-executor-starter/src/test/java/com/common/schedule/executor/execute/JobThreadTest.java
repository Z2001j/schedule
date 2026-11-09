package com.common.schedule.executor.execute;

import com.common.schedule.executor.annotation.BlockStrategy;
import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.model.TriggerParam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class JobThreadTest {

    public static class SampleJob {
        final AtomicInteger count = new AtomicInteger();

        public void ok() {
            count.incrementAndGet();
        }

        public void boom() {
            throw new IllegalStateException("业务异常");
        }
    }

    public static class BlockingJob {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        public void block() throws InterruptedException {
            started.countDown();
            release.await();
        }
    }

    public static class ShardingJob {
        int index;
        int total;

        public void run() {
            index = ShardingContext.getShardIndex();
            total = ShardingContext.getShardTotal();
        }
    }

    private static TriggerParam param(long logId, BlockStrategy strategy) {
        TriggerParam p = new TriggerParam();
        p.setTaskId("t");
        p.setLogId(logId);
        p.setBlockStrategy(strategy);
        p.setShardIndex(0);
        p.setShardTotal(1);
        return p;
    }

    @Test
    void executesAndCallbacksSuccess() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        SampleJob job = new SampleJob();
        Method m = SampleJob.class.getMethod("ok");
        JobThread thread = new JobThread("t-ok", job, m, admin);
        thread.start();
        try {
            thread.push(param(1, BlockStrategy.SERIAL));
            verify(admin, timeout(2000)).callback(eq(1L), eq(true), anyLong(), isNull());
            assertEquals(1, job.count.get());
        } finally {
            thread.shutdown();
        }
    }

    @Test
    void exceptionIsolatedAndThreadSurvives() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        SampleJob job = new SampleJob();
        Method m = SampleJob.class.getMethod("boom");
        JobThread thread = new JobThread("t-boom", job, m, admin);
        thread.start();
        try {
            thread.push(param(2, BlockStrategy.SERIAL));
            thread.push(param(3, BlockStrategy.SERIAL));
            verify(admin, timeout(2000)).callback(eq(2L), eq(false), anyLong(), anyString());
            verify(admin, timeout(2000)).callback(eq(3L), eq(false), anyLong(), anyString());
            assertTrue(thread.isAlive(), "异常不应导致执行线程退出");
        } finally {
            thread.shutdown();
        }
    }

    @Test
    void shardingContextVisibleDuringExecution() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        ShardingJob job = new ShardingJob();
        Method m = ShardingJob.class.getMethod("run");
        JobThread thread = new JobThread("t-shard", job, m, admin);
        thread.start();
        try {
            TriggerParam p = param(4, BlockStrategy.SERIAL);
            p.setShardIndex(2);
            p.setShardTotal(3);
            thread.push(p);
            verify(admin, timeout(2000)).callback(eq(4L), eq(true), anyLong(), isNull());
            assertEquals(2, job.index);
            assertEquals(3, job.total);
        } finally {
            thread.shutdown();
        }
    }

    @Test
    void discardWhenBusy() throws Exception {
        AdminClient admin = mock(AdminClient.class);
        BlockingJob job = new BlockingJob();
        Method m = BlockingJob.class.getMethod("block");
        JobThread thread = new JobThread("t-block", job, m, admin);
        thread.start();
        try {
            thread.push(param(10, BlockStrategy.DISCARD));
            assertTrue(job.started.await(2, TimeUnit.SECONDS));
            assertTrue(!thread.isIdle(), "首个任务执行中应处于忙碌态");

            thread.push(param(11, BlockStrategy.DISCARD));
            verify(admin, timeout(2000)).callback(eq(11L), eq(false), eq(0L), contains("丢弃"));

            job.release.countDown();
            verify(admin, timeout(2000)).callback(eq(10L), eq(true), anyLong(), isNull());
        } finally {
            job.release.countDown();
            thread.shutdown();
        }
    }
}