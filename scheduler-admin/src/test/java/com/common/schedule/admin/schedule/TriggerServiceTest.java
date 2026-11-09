package com.common.schedule.admin.schedule;

import com.common.schedule.admin.alarm.AlarmNotifier;
import com.common.schedule.admin.client.ExecutorClient;
import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.model.TriggerParam;
import com.common.schedule.admin.route.ExecutorRouter;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.storage.TaskLogDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriggerServiceTest {

    private TaskConfigDao taskConfigDao;
    private ExecutorDao executorDao;
    private ExecutorRouter router;
    private ExecutorClient client;
    private TaskLogDao logDao;
    private AlarmNotifier alarmNotifier;
    private TriggerService service;

    @BeforeEach
    void setup() {
        taskConfigDao = mock(TaskConfigDao.class);
        executorDao = mock(ExecutorDao.class);
        router = mock(ExecutorRouter.class);
        client = mock(ExecutorClient.class);
        logDao = mock(TaskLogDao.class);
        alarmNotifier = mock(AlarmNotifier.class);
        service = new TriggerService(taskConfigDao, executorDao, router, client, logDao, alarmNotifier);
    }

    private static TaskConfig config(boolean enabled, int retry) {
        TaskConfig c = new TaskConfig();
        c.setTaskId("app:bean.run");
        c.setAppName("app");
        c.setEnabled(enabled);
        c.setRouteStrategy(RouteStrategy.FIRST);
        c.setBlockStrategy(BlockStrategy.SERIAL);
        c.setRetryCount(retry);
        return c;
    }

    private static ExecutorNode node(String address) {
        ExecutorNode n = new ExecutorNode();
        n.setAddress(address);
        return n;
    }

    @Test
    void singleTargetSuccess() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0));
        when(executorDao.findOnlineByApp("app")).thenReturn(List.of(node("http://a:1")));
        when(router.route(any(), anyList(), anyString())).thenReturn(List.of(new RouteTarget("http://a:1", 0, 1)));
        when(logDao.insertTrigger(any(), anyString())).thenReturn(100L);
        when(client.run(eq("http://a:1"), any(TriggerParam.class))).thenReturn(true);

        service.trigger("app:bean.run", TriggerService.TYPE_MANUAL);

        verify(logDao).markTriggered(100L, "http://a:1", 0, 1);
        verify(logDao, never()).markTriggerFail(anyLong(), anyString());
        verify(alarmNotifier, never()).onTriggerFailure(any(TaskConfig.class), anyString(), anyString());
    }

    @Test
    void noOnlineNodeManualLogsFail() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0));
        when(executorDao.findOnlineByApp("app")).thenReturn(List.of());
        when(router.route(any(), anyList(), anyString())).thenReturn(List.of());
        when(logDao.insertTrigger(any(), anyString())).thenReturn(101L);

        service.trigger("app:bean.run", TriggerService.TYPE_MANUAL);

        verify(logDao).markTriggerFail(eq(101L), anyString());
        verify(client, never()).run(anyString(), any());
        verify(alarmNotifier).onTriggerFailure(any(TaskConfig.class), eq(TriggerService.TYPE_MANUAL), eq("无在线执行器实例"));
    }

    @Test
    void noOnlineNodeScheduleSkipsLog() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0));
        when(executorDao.findOnlineByApp("app")).thenReturn(List.of());
        when(router.route(any(), anyList(), anyString())).thenReturn(List.of());

        service.trigger("app:bean.run", TriggerService.TYPE_SCHEDULE);

        verify(logDao, never()).insertTrigger(any(), anyString());
        verify(alarmNotifier).onTriggerFailure(any(TaskConfig.class), eq(TriggerService.TYPE_SCHEDULE), eq("无在线执行器实例"));
    }

    @Test
    void shardingBroadcastCreatesLogPerShard() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 0));
        when(executorDao.findOnlineByApp("app")).thenReturn(List.of(node("http://a:1"), node("http://b:2")));
        when(router.route(any(), anyList(), anyString())).thenReturn(List.of(
                new RouteTarget("http://a:1", 0, 2), new RouteTarget("http://b:2", 1, 2)));
        when(logDao.insertTrigger(any(), anyString())).thenReturn(200L, 201L);
        when(client.run(anyString(), any(TriggerParam.class))).thenReturn(true);

        service.trigger("app:bean.run", TriggerService.TYPE_SCHEDULE);

        verify(logDao, times(2)).insertTrigger(any(), anyString());
        verify(client, times(2)).run(anyString(), any(TriggerParam.class));
        verify(logDao).markTriggered(200L, "http://a:1", 0, 2);
        verify(logDao).markTriggered(201L, "http://b:2", 1, 2);
    }

    @Test
    void runFailureRetriesThenMarksFail() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(true, 1));
        when(executorDao.findOnlineByApp("app")).thenReturn(List.of(node("http://a:1")));
        when(router.route(any(), anyList(), anyString())).thenReturn(List.of(new RouteTarget("http://a:1", 0, 1)));
        when(logDao.insertTrigger(any(), anyString())).thenReturn(300L);
        when(client.run(anyString(), any(TriggerParam.class))).thenReturn(false);

        service.trigger("app:bean.run", TriggerService.TYPE_MANUAL);

        verify(client, times(2)).run(anyString(), any(TriggerParam.class));
        verify(logDao).markTriggerFail(eq(300L), anyString());
        verify(logDao, never()).markTriggered(anyLong(), anyString(), anyInt(), anyInt());
        verify(alarmNotifier).onTriggerFailure(any(TaskConfig.class), eq(TriggerService.TYPE_MANUAL), anyString());
    }

    @Test
    void disabledScheduleReturnsEarly() {
        when(taskConfigDao.findById("app:bean.run")).thenReturn(config(false, 0));

        service.trigger("app:bean.run", TriggerService.TYPE_SCHEDULE);

        verify(executorDao, never()).findOnlineByApp(anyString());
        verify(logDao, never()).insertTrigger(any(), anyString());
    }

    @Test
    void missingTaskIsIgnored() {
        when(taskConfigDao.findById("missing")).thenReturn(null);
        service.trigger("missing", TriggerService.TYPE_MANUAL);
        verify(executorDao, never()).findOnlineByApp(anyString());
    }
}