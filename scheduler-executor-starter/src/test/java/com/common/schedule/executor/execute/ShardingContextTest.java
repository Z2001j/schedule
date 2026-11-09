package com.common.schedule.executor.execute;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShardingContextTest {

    @AfterEach
    void tearDown() {
        ShardingContext.clear();
    }

    @Test
    void defaultsWhenNotSet() {
        assertEquals(0, ShardingContext.getShardIndex());
        assertEquals(1, ShardingContext.getShardTotal());
    }

    @Test
    void setAndRead() {
        ShardingContext.set(2, 5);
        assertEquals(2, ShardingContext.getShardIndex());
        assertEquals(5, ShardingContext.getShardTotal());
    }

    @Test
    void clearRestoresDefaults() {
        ShardingContext.set(1, 3);
        ShardingContext.clear();
        assertEquals(0, ShardingContext.getShardIndex());
        assertEquals(1, ShardingContext.getShardTotal());
    }

    @Test
    void isolatedAcrossThreads() throws InterruptedException {
        ShardingContext.set(1, 4);
        int[] other = new int[2];
        Thread t = new Thread(() -> {
            other[0] = ShardingContext.getShardIndex();
            other[1] = ShardingContext.getShardTotal();
        });
        t.start();
        t.join();
        assertEquals(0, other[0]);
        assertEquals(1, other[1]);
        assertEquals(1, ShardingContext.getShardIndex());
        assertEquals(4, ShardingContext.getShardTotal());
    }
}