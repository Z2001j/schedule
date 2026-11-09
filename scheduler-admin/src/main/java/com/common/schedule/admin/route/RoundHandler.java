package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundHandler implements RouteStrategyHandler {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public RouteStrategy strategy() {
        return RouteStrategy.ROUND;
    }

    @Override
    public List<RouteTarget> route(List<ExecutorNode> nodes, String taskId) {
        int index = Math.floorMod(counters.computeIfAbsent(taskId, k -> new AtomicInteger()).getAndIncrement(), nodes.size());
        return List.of(new RouteTarget(nodes.get(index).getAddress(), 0, 1));
    }
}