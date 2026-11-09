package com.common.schedule.admin.storage;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务配置持久化（JdbcTemplate）。updateWithVersion 实现乐观锁。
 */
@Repository
public class TaskConfigDao {

    private final JdbcTemplate jdbc;
    private final AdminProperties properties;

    private final RowMapper<TaskConfig> mapper = (rs, n) -> {
        TaskConfig c = new TaskConfig();
        c.setTaskId(rs.getString("task_id"));
        c.setAppName(rs.getString("app_name"));
        c.setTaskName(rs.getString("task_name"));
        c.setTaskGroup(rs.getString("task_group"));
        c.setDescription(rs.getString("description"));
        c.setScheduleType(ScheduleType.valueOf(rs.getString("schedule_type")));
        c.setCronExpression(rs.getString("cron_expression"));
        c.setFixedRate(nullableLong(rs, "fixed_rate"));
        c.setFixedDelay(nullableLong(rs, "fixed_delay"));
        c.setInitialDelay(nullableLong(rs, "initial_delay"));
        c.setEnabled(rs.getBoolean("enabled"));
        c.setRouteStrategy(RouteStrategy.valueOf(rs.getString("route_strategy")));
        c.setBlockStrategy(BlockStrategy.valueOf(rs.getString("block_strategy")));
        c.setRetryCount(rs.getInt("retry_count"));
        c.setVersion(rs.getInt("version"));
        c.setCreateTime(toLocal(rs.getTimestamp("create_time")));
        c.setUpdateTime(toLocal(rs.getTimestamp("update_time")));
        return c;
    };

    public TaskConfigDao(JdbcTemplate jdbc, AdminProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    private String table() {
        return properties.getTableName();
    }

    public TaskConfig findById(String taskId) {
        List<TaskConfig> list = jdbc.query("SELECT * FROM " + table() + " WHERE task_id=?", mapper, taskId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<TaskConfig> findAll() {
        return findAll(null, null, null, null);
    }

    /**
     * 服务端过滤：appName/group 精确匹配，enabled 精确匹配，keyword 模糊匹配 task_name 或 task_id。
     * 参数为空则忽略该条件。全部参数化，避免 SQL 注入。
     */
    public List<TaskConfig> findAll(String appName, String keyword, String group, Boolean enabled) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (appName != null && !appName.isBlank()) {
            sql.append(" AND app_name=?");
            args.add(appName.trim());
        }
        if (group != null && !group.isBlank()) {
            sql.append(" AND task_group=?");
            args.add(group.trim());
        }
        if (enabled != null) {
            sql.append(" AND enabled=?");
            args.add(enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (task_name LIKE ? OR task_id LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY app_name, task_id");
        return jdbc.query(sql.toString(), mapper, args.toArray());
    }

    public void insert(TaskConfig c) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update("INSERT INTO " + table() + " (task_id,app_name,task_name,task_group,schedule_type,"
                        + "cron_expression,fixed_rate,fixed_delay,initial_delay,enabled,route_strategy,block_strategy,"
                        + "retry_count,description,version,create_time,update_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                c.getTaskId(), c.getAppName(), c.getTaskName(), c.getTaskGroup(), c.getScheduleType().name(),
                c.getCronExpression(), c.getFixedRate(), c.getFixedDelay(),
                c.getInitialDelay() == null ? 0 : c.getInitialDelay(), c.isEnabled(),
                c.getRouteStrategy().name(), c.getBlockStrategy().name(), c.getRetryCount(), c.getDescription(),
                0, now, now);
    }

    /**
     * 乐观锁更新：WHERE version=? 命中才更新，version 自增。返回受影响行数（0 表示冲突）。
     */
    public int updateWithVersion(TaskConfig c) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        return jdbc.update("UPDATE " + table() + " SET task_name=?,task_group=?,description=?,schedule_type=?,"
                        + "cron_expression=?,fixed_rate=?,fixed_delay=?,initial_delay=?,enabled=?,route_strategy=?,"
                        + "block_strategy=?,retry_count=?,version=version+1,update_time=? WHERE task_id=? AND version=?",
                c.getTaskName(), c.getTaskGroup(), c.getDescription(), c.getScheduleType().name(),
                c.getCronExpression(), c.getFixedRate(), c.getFixedDelay(),
                c.getInitialDelay() == null ? 0 : c.getInitialDelay(), c.isEnabled(),
                c.getRouteStrategy().name(), c.getBlockStrategy().name(), c.getRetryCount(),
                now, c.getTaskId(), c.getVersion());
    }

    private static Long nullableLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}