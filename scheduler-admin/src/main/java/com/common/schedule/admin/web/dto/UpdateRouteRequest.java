package com.common.schedule.admin.web.dto;

import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.RouteStrategy;

/**
 * 修改路由/阻塞/重试策略请求。
 */
public class UpdateRouteRequest {

    private RouteStrategy routeStrategy;
    private BlockStrategy blockStrategy;
    private int retryCount;

    public RouteStrategy getRouteStrategy() {
        return routeStrategy;
    }

    public void setRouteStrategy(RouteStrategy routeStrategy) {
        this.routeStrategy = routeStrategy;
    }

    public BlockStrategy getBlockStrategy() {
        return blockStrategy;
    }

    public void setBlockStrategy(BlockStrategy blockStrategy) {
        this.blockStrategy = blockStrategy;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}