package com.common.schedule.admin.registry;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.storage.ExecutorDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistryReaperTest {

    @Test
    void reapMarksOfflineBeforeHeartbeatThreshold() {
        ExecutorDao executorDao = mock(ExecutorDao.class);
        when(executorDao.markOfflineBefore(any())).thenReturn(2);
        AdminProperties properties = new AdminProperties();
        properties.setHeartbeatTimeout(90);
        RegistryReaper reaper = new RegistryReaper(executorDao, properties);

        LocalDateTime before = LocalDateTime.now().minusSeconds(90);
        reaper.reap();
        LocalDateTime after = LocalDateTime.now().minusSeconds(90);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(executorDao).markOfflineBefore(captor.capture());
        assertThat(captor.getValue()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }
}