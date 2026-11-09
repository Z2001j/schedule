package com.common.schedule.admin.route;

import com.common.schedule.admin.client.ExecutorClient;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 故障转移：依次空闲探测，选第一个空闲节点；全忙则回退第一个节点。
 */
@Component
public class FailoverHandler implements RouteStrategyHandler {

    private final ExecutorClient executorClient;

    public FailoverHandler(ExecutorClient executorClient) {
        this.executorClient = executorClient;
    }

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.FAILOVER;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        for (ExecutorNode node : nodes) {
            if (executorClient.beat(node.getAddress())) {
                return List.of(new RouteTarget(node.getAddress(), 0, 1));
            }
        }
        return List.of(new RouteTarget(nodes.get(0).getAddress(), 0, 1));
    }
}