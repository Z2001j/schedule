package com.common.schedule.admin.registry;

import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.schedule.AdminScheduler;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.web.dto.RegisterRequest;
import com.common.schedule.admin.web.dto.TaskMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistryServiceTest {

    private ExecutorDao executorDao;
    private TaskConfigDao taskConfigDao;
    private AdminScheduler adminScheduler;
    private RegistryService service;

    @BeforeEach
    void setup() {
        executorDao = mock(ExecutorDao.class);
        taskConfigDao = mock(TaskConfigDao.class);
        adminScheduler = mock(AdminScheduler.class);
        service = new RegistryService(executorDao, taskConfigDao, adminScheduler);
    }

    private static RegisterRequest request(TaskMeta... metas) {
        RegisterRequest r = new RegisterRequest();
        r.setAppName("app");
        r.setAddress("http://a:1");
        r.setTasks(List.of(metas));
        return r;
    }

    private static TaskMeta meta(String taskId) {
        TaskMeta m = new TaskMeta();
        m.setTaskId(taskId);
        m.setName("demo");
        m.setType(ScheduleType.CRON);
        m.setCron("0/5 * * * * ?");
        m.setEnabled(true);
        return m;
    }

    @Test
    void registerInsertsNewTaskAndSchedules() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(null);
        service.register(request(meta("app:bean.run")));

        verify(executorDao).upsertOnline("app", "http://a:1");
        ArgumentCaptor<TaskConfig> captor = ArgumentCaptor.forClass(TaskConfig.class);
        verify(taskConfigDao).insert(captor.capture());
        TaskConfig inserted = captor.getValue();
        assertThat(inserted.getTaskId()).isEqualTo("app:bean.run");
        assertThat(inserted.getAppName()).isEqualTo("app");
        assertThat(inserted.getScheduleType()).isEqualTo(ScheduleType.CRON);
        assertThat(inserted.getCronExpression()).isEqualTo("0/5 * * * * ?");
        verify(adminScheduler).schedule(any(TaskConfig.class));
    }

    @Test
    void registerDoesNotOverwriteExistingConfig() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(new TaskConfig());
        service.register(request(meta("app:bean.run")));

        verify(executorDao).upsertOnline("app", "http://a:1");
        verify(taskConfigDao, never()).insert(any());
        verify(adminScheduler, never()).schedule(any());
    }

    @Test
    void registerDisabledTaskIsNotScheduled() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(null);
        TaskMeta m = meta("app:bean.run");
        m.setEnabled(false);
        service.register(request(m));

        verify(taskConfigDao).insert(any());
        verify(adminScheduler, never()).schedule(any());
    }

    @Test
    void beatAndRemoveDelegate() {
        service.beat("app", "http://a:1");
        verify(executorDao).updateHeartbeat("app", "http://a:1");
        service.remove("app", "http://a:1");
        verify(executorDao).markOffline("app", "http://a:1");
    }

    @Test
    void onlineNodesAndAllExecutorsDelegate() {
        service.onlineNodes("app");
        verify(executorDao).findOnlineByApp("app");
        service.allExecutors();
        verify(executorDao).findAll();
    }
}