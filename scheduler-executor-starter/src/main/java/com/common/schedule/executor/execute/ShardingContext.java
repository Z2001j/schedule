package com.common.schedule.executor.execute;

/**
 * 分片上下文（ThreadLocal）。分片广播时由 JobThread 设置，业务方法内读取。
 * 非分片任务默认 index=0、total=1。
 */
public final class ShardingContext {

    private static final ThreadLocal<int[]> CONTEXT = new ThreadLocal<>();

    private ShardingContext() {
    }

    public static void set(int shardIndex, int shardTotal) {
        CONTEXT.set(new int[]{shardIndex, shardTotal});
    }

    public static int getShardIndex() {
        int[] v = CONTEXT.get();
        return v == null ? 0 : v[0];
    }

    public static int getShardTotal() {
        int[] v = CONTEXT.get();
        return v == null ? 1 : v[1];
    }

    public static void clear() {
        CONTEXT.remove();
    }
}