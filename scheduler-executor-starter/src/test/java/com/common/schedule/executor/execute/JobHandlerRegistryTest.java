package com.common.schedule.executor.execute;

import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.client.AdminClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JobHandlerRegistryTest {

    public static class Bean {
        @ManagedScheduled(name = "任务A", cron = "0 */5 * * * ?")
        public void taskA() {
        }
    }

    private JobHandler newHandler(String id) throws Exception {
        Bean bean = new Bean();
        Method m = Bean.class.getMethod("taskA");
        ManagedScheduled ann = m.getAnnotation(ManagedScheduled.class);
        return new JobHandler(id, bean, m, ann, mock(AdminClient.class));
    }

    @Test
    void registerAndGet() throws Exception {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        JobHandler h = newHandler("app:bean.taskA");
        registry.register(h);
        assertTrue(registry.contains("app:bean.taskA"));
        assertSame(h, registry.get("app:bean.taskA"));
        assertEquals(1, registry.size());
        assertEquals(1, registry.all().size());
    }

    @Test
    void missingReturnsNull() {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        assertFalse(registry.contains("nope"));
        assertNull(registry.get("nope"));
        assertEquals(0, registry.size());
    }

    @Test
    void startAllAndStopAll() throws Exception {
        JobHandlerRegistry registry = new JobHandlerRegistry();
        registry.register(newHandler("t1"));
        registry.register(newHandler("t2"));
        registry.startAll();
        registry.all().forEach(h -> assertTrue(h.getJobThread().isAlive()));
        registry.stopAll();
    }
}