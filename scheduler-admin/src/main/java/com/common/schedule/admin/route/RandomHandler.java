package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomHandler implements RouteStrategyHandler {

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.RANDOM;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        int index = ThreadLocalRandom.current().nextInt(nodes.size());
        return List.of(new RouteTarget(nodes.get(index).getAddress(), 0, 1));
    }
}