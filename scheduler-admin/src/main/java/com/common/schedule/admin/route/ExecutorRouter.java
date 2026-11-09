package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 路由门面：按策略分派到对应处理器。无在线节点返回空列表。
 */
@Component
public class ExecutorRouter {

    private final Map<RouteStrategy, RouteStrategyHandler> handlers = new EnumMap<>(RouteStrategy.class);

    public ExecutorRouter(List<RouteStrategyHandler> handlerList) {
        for (RouteStrategyHandler handler : handlerList) {
            handlers.put(handler.strategy(), handler);
        }
    }

    public List<RouteTarget> route(RouteStrategy strategy, List<ExecutorNode> nodes, String taskId) {
        if (nodes == null || nodes.isEmpty()) {
            return Collections.emptyList();
        }
        RouteStrategyHandler handler = handlers.getOrDefault(strategy, handlers.get(RouteStrategy.FIRST));
        return handler.route(nodes, taskId);
    }
}