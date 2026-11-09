package com.common.schedule.admin.storage;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.model.TaskLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 触发/执行日志持久化。触发时插入并返回 logId，执行器回调后更新同一条。
 */
@Repository
public class TaskLogDao {

    private final JdbcTemplate jdbc;
    private final AdminProperties properties;

    private final RowMapper<TaskLog> mapper = (rs, n) -> {
        TaskLog l = new TaskLog();
        l.setId(rs.getLong("id"));
        l.setTaskId(rs.getString("task_id"));
        l.setTriggerTime(toLocal(rs.getTimestamp("trigger_time")));
        l.setTriggerType(rs.getString("trigger_type"));
        String route = rs.getString("route_strategy");
        l.setRouteStrategy(route == null ? null : RouteStrategy.valueOf(route));
        l.setExecutorAddress(rs.getString("executor_address"));
        l.setShardIndex(nullableInt(rs, "shard_index"));
        l.setShardTotal(nullableInt(rs, "shard_total"));
        l.setTriggerResult(rs.getString("trigger_result"));
        l.setExecStart(toLocal(rs.getTimestamp("exec_start")));
        l.setExecEnd(toLocal(rs.getTimestamp("exec_end")));
        l.setCostMs(nullableLong(rs, "cost_ms"));
        boolean success = rs.getBoolean("success");
        l.setSuccess(rs.wasNull() ? null : success);
        l.setErrorMsg(rs.getString("error_msg"));
        return l;
    };

    public TaskLogDao(JdbcTemplate jdbc, AdminProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    private String table() {
        return properties.getLogTableName();
    }

    public long insertTrigger(TaskConfig cfg, String triggerType) {
        String sql = "INSERT INTO " + table() + " (task_id,trigger_time,trigger_type,route_strategy) VALUES (?,?,?,?)";
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, cfg.getTaskId());
            ps.setTimestamp(2, now);
            ps.setString(3, triggerType);
            ps.setString(4, cfg.getRouteStrategy() == null ? null : cfg.getRouteStrategy().name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1 : key.longValue();
    }

    public void markTriggered(long logId, String address, int shardIndex, int shardTotal) {
        jdbc.update("UPDATE " + table() + " SET executor_address=?,shard_index=?,shard_total=?,trigger_result='TRIGGERED',exec_start=? WHERE id=?",
                address, shardIndex, shardTotal, Timestamp.valueOf(LocalDateTime.now()), logId);
    }

    public void markTriggerFail(long logId, String errorMsg) {
        jdbc.update("UPDATE " + table() + " SET trigger_result='FAIL',success=0,error_msg=? WHERE id=?", errorMsg, logId);
    }

    public void updateCallback(long logId, boolean success, long costMs, String errorMsg) {
        jdbc.update("UPDATE " + table() + " SET exec_end=?,cost_ms=?,success=?,error_msg=? WHERE id=?",
                Timestamp.valueOf(LocalDateTime.now()), costMs, success, errorMsg, logId);
    }

    public List<TaskLog> findByTask(String taskId, int limit, int offset) {
        return jdbc.query("SELECT * FROM " + table() + " WHERE task_id=? ORDER BY trigger_time DESC,id DESC LIMIT ? OFFSET ?",
                mapper, taskId, limit, offset);
    }

    public TaskLog findLatestByTask(String taskId) {
        List<TaskLog> list = jdbc.query("SELECT * FROM " + table() + " WHERE task_id=? ORDER BY trigger_time DESC,id DESC LIMIT 1",
                mapper, taskId);
        return list.isEmpty() ? null : list.get(0);
    }

    private static Long nullableLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static Integer nullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}