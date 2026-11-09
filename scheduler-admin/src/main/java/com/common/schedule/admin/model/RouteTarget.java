package com.common.schedule.admin.model;

/**
 * 路由选择结果：目标执行器地址 + 分片信息。
 */
public class RouteTarget {

    private final String address;
    private final int shardIndex;
    private final int shardTotal;

    public RouteTarget(String address, int shardIndex, int shardTotal) {
        this.address = address;
        this.shardIndex = shardIndex;
        this.shardTotal = shardTotal;
    }

    public String getAddress() {
        return address;
    }

    public int getShardIndex() {
        return shardIndex;
    }

    public int getShardTotal() {
        return shardTotal;
    }
}