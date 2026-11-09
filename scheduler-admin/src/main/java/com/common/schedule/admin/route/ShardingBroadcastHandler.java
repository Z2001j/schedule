package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分片广播：触发所有在线节点，shardTotal=节点数，shardIndex=序号。
 */
@Component
public class ShardingBroadcastHandler implements RouteStrategyHandler {

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.SHARDING_BROADCAST;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        List<RouteTarget> targets = new ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            targets.add(new RouteTarget(nodes.get(i).getAddress(), i, nodes.size()));
        }
        return targets;
    }
}