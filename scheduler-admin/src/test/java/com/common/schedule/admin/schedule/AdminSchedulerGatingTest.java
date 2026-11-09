package com.common.schedule.admin.schedule;

import com.common.schedule.admin.ha.AdminLeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSchedulerGatingTest {

    private TriggerService triggerService;
    private AdminLeaderElection leaderElection;
    private AdminScheduler adminScheduler;

    @BeforeEach
    void setup() {
        triggerService = mock(TriggerService.class);
        leaderElection = mock(AdminLeaderElection.class);
        adminScheduler = new AdminScheduler(mock(ThreadPoolTaskScheduler.class), triggerService, leaderElection);
    }

    @Test
    void leaderDispatchesScheduledTrigger() {
        when(leaderElection.isLeader()).thenReturn(true);
        adminScheduler.runScheduled("app:bean.run");
        verify(triggerService).trigger("app:bean.run", TriggerService.TYPE_SCHEDULE);
    }

    @Test
    void followerSkipsScheduledTrigger() {
        when(leaderElection.isLeader()).thenReturn(false);
        adminScheduler.runScheduled("app:bean.run");
        verify(triggerService, never()).trigger(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}