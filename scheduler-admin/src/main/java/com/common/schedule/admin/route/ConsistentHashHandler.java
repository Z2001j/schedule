package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 一致性哈希：按 taskId 落到哈希环上顺时针最近的节点，节点增减时仅少量任务迁移。
 */
@Component
public class ConsistentHashHandler implements RouteStrategyHandler {

    private static final int VIRTUAL_NODES = 160;

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.CONSISTENT_HASH;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        TreeMap<Long, String> ring = new TreeMap<>();
        for (ExecutorNode node : nodes) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                ring.put(hash(node.getAddress() + "#" + i), node.getAddress());
            }
        }
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash(taskId));
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return List.of(new RouteTarget(entry.getValue(), 0, 1));
    }

    /** FNV-1a 64 位哈希 */
    private static long hash(String value) {
        long hash = 0xcbf29ce484222325L;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}