package com.common.schedule.executor.registrar;

import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.client.HeartbeatWorker;
import com.common.schedule.executor.config.ExecutorProperties;
import com.common.schedule.executor.execute.JobHandlerRegistry;
import com.common.schedule.executor.model.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorRegistrarTest {

    public static class SampleTaskBean {
        @ManagedScheduled(name = "测试任务", group = "demo", cron = "0 */5 * * * ?")
        public void doWork() {
        }
    }

    @Test
    void scansAnnotationAndRegistersToAdmin() {
        AdminClient adminClient = mock(AdminClient.class);
        when(adminClient.register(any())).thenReturn(true);

        ExecutorProperties props = new ExecutorProperties();
        props.setAppName("test-app");
        props.setPort(9999);

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(ExecutorProperties.class, () -> props);
        ctx.registerBean(AdminClient.class, () -> adminClient);
        ctx.registerBean(JobHandlerRegistry.class, JobHandlerRegistry::new);
        ctx.registerBean(HeartbeatWorker.class, () -> new HeartbeatWorker(adminClient, "test-app", 30));
        ctx.registerBean(SampleTaskBean.class, SampleTaskBean::new);
        ctx.registerBean(ExecutorRegistrar.class, () -> new ExecutorRegistrar(
                ctx.getBean(JobHandlerRegistry.class), adminClient,
                ctx.getBean(HeartbeatWorker.class), props));
        ctx.refresh();
        try {
            JobHandlerRegistry registry = ctx.getBean(JobHandlerRegistry.class);
            assertEquals(1, registry.size());

            ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
            verify(adminClient).register(captor.capture());
            RegisterRequest req = captor.getValue();
            assertEquals("test-app", req.getAppName());
            assertEquals(1, req.getTasks().size());
            assertEquals("测试任务", req.getTasks().get(0).getName());
            String taskId = req.getTasks().get(0).getTaskId();
            assertTrue(taskId.endsWith(".doWork"));
            assertTrue(registry.contains(taskId));
        } finally {
            ctx.close();
        }
    }
}