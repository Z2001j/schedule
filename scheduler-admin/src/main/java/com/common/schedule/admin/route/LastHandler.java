package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LastHandler implements RouteStrategyHandler {

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.LAST;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        return List.of(new RouteTarget(nodes.get(nodes.size() - 1).getAddress(), 0, 1));
    }
}