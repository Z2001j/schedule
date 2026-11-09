package com.common.schedule.admin.storage;

import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.model.TaskLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DaoIntegrationTest extends AbstractDaoTest {

    private static TaskConfig config(String taskId) {
        TaskConfig c = new TaskConfig();
        c.setTaskId(taskId);
        c.setAppName("app");
        c.setTaskName("demo");
        c.setTaskGroup("default");
        c.setScheduleType(ScheduleType.CRON);
        c.setCronExpression("0/5 * * * * ?");
        c.setInitialDelay(0L);
        c.setEnabled(true);
        c.setRouteStrategy(RouteStrategy.FAILOVER);
        c.setBlockStrategy(BlockStrategy.SERIAL);
        c.setRetryCount(0);
        return c;
    }

    @Test
    void taskConfigInsertFindAll() {
        TaskConfigDao dao = new TaskConfigDao(jdbc, properties);
        dao.insert(config("app:bean.run"));
        dao.insert(config("app:bean.other"));

        TaskConfig found = dao.findById("app:bean.run");
        assertThat(found).isNotNull();
        assertThat(found.getScheduleType()).isEqualTo(ScheduleType.CRON);
        assertThat(found.getCronExpression()).isEqualTo("0/5 * * * * ?");
        assertThat(found.getVersion()).isZero();
        assertThat(dao.findAll()).hasSize(2);
        assertThat(dao.findById("absent")).isNull();
    }

    @Test
    void taskConfigServerSideFilter() {
        TaskConfigDao dao = new TaskConfigDao(jdbc, properties);
        dao.insert(config("app:bean.run"));
        TaskConfig stopped = config("app:bean.stop");
        stopped.setEnabled(false);
        stopped.setTaskGroup("batch");
        stopped.setTaskName("夜间批处理");
        dao.insert(stopped);
        TaskConfig other = config("other:bean.run");
        other.setAppName("other");
        dao.insert(other);

        assertThat(dao.findAll(null, null, null, null)).hasSize(3);
        assertThat(dao.findAll("app", null, null, null)).hasSize(2);
        assertThat(dao.findAll(null, null, "batch", null)).hasSize(1);
        assertThat(dao.findAll(null, null, null, false)).hasSize(1);
        assertThat(dao.findAll("app", null, null, true)).hasSize(1);
        assertThat(dao.findAll(null, "批处理", null, null)).hasSize(1);
        assertThat(dao.findAll(null, "bean.run", null, null)).hasSize(2);
        assertThat(dao.findAll("app", "stop", "batch", false)).hasSize(1);
        assertThat(dao.findAll("absent-app", null, null, null)).isEmpty();
    }

    @Test
    void taskConfigOptimisticLock() {
        TaskConfigDao dao = new TaskConfigDao(jdbc, properties);
        dao.insert(config("app:bean.run"));

        TaskConfig c = dao.findById("app:bean.run");
        c.setTaskName("renamed");
        c.setVersion(0);
        assertThat(dao.updateWithVersion(c)).isEqualTo(1);
        assertThat(dao.findById("app:bean.run").getVersion()).isEqualTo(1);
        assertThat(dao.findById("app:bean.run").getTaskName()).isEqualTo("renamed");

        // 持旧版本再次更新 → 冲突，0 行
        c.setVersion(0);
        c.setTaskName("stale");
        assertThat(dao.updateWithVersion(c)).isZero();
        assertThat(dao.findById("app:bean.run").getTaskName()).isEqualTo("renamed");
    }

    @Test
    void executorUpsertHeartbeatAndOffline() {
        ExecutorDao dao = new ExecutorDao(jdbc, properties);
        dao.upsertOnline("app", "http://a:1");
        dao.upsertOnline("app", "http://a:1"); // 续约不新增行
        dao.upsertOnline("app", "http://b:2");

        List<ExecutorNode> online = dao.findOnlineByApp("app");
        assertThat(online).hasSize(2);
        assertThat(dao.findAll()).hasSize(2);

        dao.markOffline("app", "http://a:1");
        assertThat(dao.findOnlineByApp("app")).hasSize(1);

        // 阈值取未来时间，剩余在线节点心跳均早于阈值 → 全部下线
        int reaped = dao.markOfflineBefore(LocalDateTime.now().plusSeconds(60));
        assertThat(reaped).isEqualTo(1);
        assertThat(dao.findOnlineByApp("app")).isEmpty();
    }

    @Test
    void taskLogTriggerCallbackAndQuery() {
        TaskConfigDao configDao = new TaskConfigDao(jdbc, properties);
        configDao.insert(config("app:bean.run"));
        TaskConfig cfg = configDao.findById("app:bean.run");
        TaskLogDao logDao = new TaskLogDao(jdbc, properties);

        long logId = logDao.insertTrigger(cfg, "MANUAL");
        assertThat(logId).isPositive();

        logDao.markTriggered(logId, "http://a:1", 0, 1);
        logDao.updateCallback(logId, true, 123L, null);

        TaskLog latest = logDao.findLatestByTask("app:bean.run");
        assertThat(latest.getId()).isEqualTo(logId);
        assertThat(latest.getExecutorAddress()).isEqualTo("http://a:1");
        assertThat(latest.getTriggerResult()).isEqualTo("TRIGGERED");
        assertThat(latest.getSuccess()).isTrue();
        assertThat(latest.getCostMs()).isEqualTo(123L);

        long second = logDao.insertTrigger(cfg, "SCHEDULE");
        logDao.markTriggerFail(second, "执行器不可达");
        List<TaskLog> logs = logDao.findByTask("app:bean.run", 10, 0);
        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).getId()).isEqualTo(second);
        assertThat(logs.get(0).getTriggerResult()).isEqualTo("FAIL");
        assertThat(logs.get(0).getSuccess()).isFalse();
    }
}