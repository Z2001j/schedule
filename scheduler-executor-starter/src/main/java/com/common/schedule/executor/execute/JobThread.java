package com.common.schedule.executor.execute;

import com.common.schedule.executor.annotation.BlockStrategy;
import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 每个任务一个独立线程 + 阻塞队列，串行消费触发指令。
 * 关键：反射调用异常被捕获并回调失败，绝不向上抛出导致线程退出、后续触发丢失。
 */
public class JobThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(JobThread.class);

    private final String taskId;
    private final Object bean;
    private final Method method;
    private final AdminClient adminClient;
    private final BlockingQueue<TriggerParam> queue = new LinkedBlockingQueue<>();

    private volatile boolean running = true;
    private volatile boolean busy = false;

    public JobThread(String taskId, Object bean, Method method, AdminClient adminClient) {
        super("sched-job-" + taskId);
        this.taskId = taskId;
        this.bean = bean;
        this.method = method;
        this.adminClient = adminClient;
        setDaemon(true);
    }

    /**
     * 接收触发指令，按阻塞策略入队。
     */
    public void push(TriggerParam param) {
        if (!running) {
            return;
        }
        BlockStrategy strategy = param.getBlockStrategy() == null ? BlockStrategy.SERIAL : param.getBlockStrategy();
        if (busy) {
            if (strategy == BlockStrategy.DISCARD) {
                adminClient.callback(param.getLogId(), false, 0, "任务忙碌，丢弃本次触发");
                return;
            }
            if (strategy == BlockStrategy.OVERRIDE) {
                queue.clear();
            }
        }
        queue.offer(param);
    }

    @Override
    public void run() {
        while (running) {
            TriggerParam param;
            try {
                param = queue.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
                continue;
            }
            if (param == null) {
                continue;
            }
            busy = true;
            long start = System.currentTimeMillis();
            boolean success = false;
            String error = null;
            try {
                ShardingContext.set(param.getShardIndex(), param.getShardTotal());
                method.invoke(bean);
                success = true;
            } catch (InvocationTargetException e) {
                error = rootMessage(e.getTargetException());
                log.error("[scheduler-executor] 任务 {} 执行失败", taskId, e.getTargetException());
            } catch (Throwable t) {
                error = rootMessage(t);
                log.error("[scheduler-executor] 任务 {} 执行异常", taskId, t);
            } finally {
                ShardingContext.clear();
                busy = false;
                long cost = System.currentTimeMillis() - start;
                try {
                    adminClient.callback(param.getLogId(), success, cost, error);
                } catch (Throwable t) {
                    log.warn("[scheduler-executor] 任务 {} 结果回调失败 logId={}", taskId, param.getLogId(), t);
                }
            }
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    public boolean isIdle() {
        return !busy && queue.isEmpty();
    }

    public String getTaskId() {
        return taskId;
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return cur.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
    }
}