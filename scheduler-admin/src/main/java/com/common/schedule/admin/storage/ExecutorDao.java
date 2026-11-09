package com.common.schedule.admin.storage;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.ExecutorNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行器注册表持久化。
 */
@Repository
public class ExecutorDao {

    private final JdbcTemplate jdbc;
    private final AdminProperties properties;

    private final RowMapper<ExecutorNode> mapper = (rs, n) -> {
        ExecutorNode e = new ExecutorNode();
        e.setId(rs.getLong("id"));
        e.setAppName(rs.getString("app_name"));
        e.setAddress(rs.getString("address"));
        e.setStatus(rs.getString("status"));
        Timestamp hb = rs.getTimestamp("last_heartbeat");
        e.setLastHeartbeat(hb == null ? null : hb.toLocalDateTime());
        Timestamp ct = rs.getTimestamp("create_time");
        e.setCreateTime(ct == null ? null : ct.toLocalDateTime());
        return e;
    };

    public ExecutorDao(JdbcTemplate jdbc, AdminProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    private String table() {
        return properties.getExecutorTableName();
    }

    /**
     * 注册/续约：存在则置在线并刷新心跳，不存在则插入。避免方言差异（不用 ON DUPLICATE KEY）。
     */
    public void upsertOnline(String appName, String address) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        int rows = jdbc.update("UPDATE " + table() + " SET status='ONLINE',last_heartbeat=? WHERE app_name=? AND address=?",
                now, appName, address);
        if (rows == 0) {
            jdbc.update("INSERT INTO " + table() + " (app_name,address,status,last_heartbeat,create_time) VALUES (?,?,?,?,?)",
                    appName, address, ExecutorNode.ONLINE, now, now);
        }
    }

    public void updateHeartbeat(String appName, String address) {
        jdbc.update("UPDATE " + table() + " SET status='ONLINE',last_heartbeat=? WHERE app_name=? AND address=?",
                Timestamp.valueOf(LocalDateTime.now()), appName, address);
    }

    public void markOffline(String appName, String address) {
        jdbc.update("UPDATE " + table() + " SET status='OFFLINE' WHERE app_name=? AND address=?", appName, address);
    }

    public int markOfflineBefore(LocalDateTime threshold) {
        return jdbc.update("UPDATE " + table() + " SET status='OFFLINE' WHERE status='ONLINE' AND last_heartbeat<?",
                Timestamp.valueOf(threshold));
    }

    public List<ExecutorNode> findOnlineByApp(String appName) {
        return jdbc.query("SELECT * FROM " + table() + " WHERE app_name=? AND status='ONLINE' ORDER BY address",
                mapper, appName);
    }

    public List<ExecutorNode> findAll() {
        return jdbc.query("SELECT * FROM " + table() + " ORDER BY app_name,address", mapper);
    }
}