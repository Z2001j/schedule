package com.common.schedule.admin.route;

import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;

import java.util.List;

/**
 * 路由策略处理器。从在线执行器节点中选择触发目标。
 */
public interface RouteStrategyHandler {

    RouteStrategy strategy();

    List<RouteTarget> route(List<ExecutorNode> nodes, String taskId);
}