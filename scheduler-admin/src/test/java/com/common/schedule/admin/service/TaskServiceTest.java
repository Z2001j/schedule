package com.common.schedule.admin.service;

import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.schedule.AdminScheduler;
import com.common.schedule.admin.schedule.TriggerService;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.storage.TaskLogDao;
import com.common.schedule.admin.web.dto.UpdateRouteRequest;
import com.common.schedule.admin.web.dto.UpdateScheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private TaskConfigDao taskConfigDao;
    private TaskLogDao taskLogDao;
    private ExecutorDao executorDao;
    private AdminScheduler adminScheduler;
    private TriggerService triggerService;
    private ScheduleValidator validator;
    private TaskService service;

    @BeforeEach
    void setup() {
        taskConfigDao = mock(TaskConfigDao.class);
        taskLogDao = mock(TaskLogDao.class);
        executorDao = mock(ExecutorDao.class);
        adminScheduler = mock(AdminScheduler.class);
        triggerService = mock(TriggerService.class);
        validator = mock(ScheduleValidator.class);
        when(executorDao.findOnlineByApp(anyString())).thenReturn(List.of());
        service = new TaskService(taskConfigDao, taskLogDao, executorDao, adminScheduler, triggerService, validator);
    }

    private static TaskConfig config(boolean enabled, int version) {
        TaskConfig c = new TaskConfig();
        c.setTaskId("app:bean.run");
        c.setAppName("app");
        c.setTaskName("demo");
        c.setEnabled(enabled);
        c.setVersion(version);
        c.setScheduleType(ScheduleType.CRON);
        c.setCronExpression("0/5 * * * * ?");
        c.setRouteStrategy(RouteStrategy.FAILOVER);
        c.setBlockStrategy(BlockStrategy.SERIAL);
        return c;
    }

    private static UpdateScheduleRequest scheduleRequest() {
        UpdateScheduleRequest r = new UpdateScheduleRequest();
        r.setType(ScheduleType.CRON);
        r.setCron("0/10 * * * * ?");
        return r;
    }

    @Test
    void updateScheduleConflictThrows() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 3));
        when(taskConfigDao.updateWithVersion(any())).thenReturn(0);

        assertThatThrownBy(() -> service.updateSchedule("app:bean.run", scheduleRequest(), 3))
                .isInstanceOf(ScheduleException.class);
        verify(adminScheduler, never()).reschedule(any());
    }

    @Test
    void updateScheduleSuccessReschedules() {
        TaskConfig before = config(true, 0);
        TaskConfig after = config(true, 1);
        after.setCronExpression("0/10 * * * * ?");
        when(taskConfigDao.findById("app:bean.run")).thenReturn(before, after);
        when(taskConfigDao.updateWithVersion(any())).thenReturn(1);

        service.updateSchedule("app:bean.run", scheduleRequest(), 0);

        ArgumentCaptor<TaskConfig> captor = ArgumentCaptor.forClass(TaskConfig.class);
        verify(taskConfigDao).updateWithVersion(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(0);
        assertThat(captor.getValue().getCronExpression()).isEqualTo("0/10 * * * * ?");
        verify(adminScheduler).reschedule(any(TaskConfig.class));
    }

    @Test
    void startEnablesAndSchedules() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(false, 0), config(true, 1));
        when(taskConfigDao.updateWithVersion(any())).thenReturn(1);

        service.start("app:bean.run", 0);

        ArgumentCaptor<TaskConfig> captor = ArgumentCaptor.forClass(TaskConfig.class);
        verify(taskConfigDao).updateWithVersion(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
        verify(adminScheduler).schedule(any(TaskConfig.class));
    }

    @Test
    void stopDisablesAndCancels() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0), config(false, 1));
        when(taskConfigDao.updateWithVersion(any())).thenReturn(1);

        service.stop("app:bean.run", 0);

        verify(adminScheduler).stop("app:bean.run");
    }

    @Test
    void updateRouteDoesNotReschedule() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0), config(true, 1));
        when(taskConfigDao.updateWithVersion(any())).thenReturn(1);
        UpdateRouteRequest r = new UpdateRouteRequest();
        r.setRouteStrategy(RouteStrategy.ROUND);
        r.setBlockStrategy(BlockStrategy.DISCARD);
        r.setRetryCount(2);

        service.updateRoute("app:bean.run", r, 0);

        verify(adminScheduler, never()).reschedule(any());
        verify(adminScheduler, never()).schedule(any());
    }

    @Test
    void detailNotFoundThrows() {
        when(taskConfigDao.findById("missing")).thenReturn(null);
        assertThatThrownBy(() -> service.detail("missing")).isInstanceOf(ScheduleException.class);
    }

    @Test
    void triggerDelegatesAsManual() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0));
        service.trigger("app:bean.run");
        verify(triggerService).trigger("app:bean.run", TriggerService.TYPE_MANUAL);
    }
}