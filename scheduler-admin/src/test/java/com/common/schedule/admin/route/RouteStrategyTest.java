package com.common.schedule.admin.route;

import com.common.schedule.admin.client.ExecutorClient;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.RouteTarget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteStrategyTest {

    private static ExecutorNode node(String address) {
        ExecutorNode n = new ExecutorNode();
        n.setAppName("app");
        n.setAddress(address);
        return n;
    }

    private static List<ExecutorNode> nodes() {
        return List.of(node("http://a:1"), node("http://b:2"), node("http://c:3"));
    }

    @Test
    void firstPicksHead() {
        List<RouteTarget> targets = new FirstHandler().route(nodes(), "t");
        assertThat(targets).hasSize(1);
        assertThat(targets.get(0).getAddress()).isEqualTo("http://a:1");
        assertThat(targets.get(0).getShardTotal()).isEqualTo(1);
    }

    @Test
    void lastPicksTail() {
        assertThat(new LastHandler().route(nodes(), "t").get(0).getAddress()).isEqualTo("http://c:3");
    }

    @Test
    void roundCyclesPerTask() {
        RoundHandler handler = new RoundHandler();
        List<ExecutorNode> nodes = nodes();
        assertThat(handler.route(nodes, "t").get(0).getAddress()).isEqualTo("http://a:1");
        assertThat(handler.route(nodes, "t").get(0).getAddress()).isEqualTo("http://b:2");
        assertThat(handler.route(nodes, "t").get(0).getAddress()).isEqualTo("http://c:3");
        assertThat(handler.route(nodes, "t").get(0).getAddress()).isEqualTo("http://a:1");
        // 不同任务计数独立
        assertThat(handler.route(nodes, "other").get(0).getAddress()).isEqualTo("http://a:1");
    }

    @Test
    void randomReturnsOneOfNodes() {
        RandomHandler handler = new RandomHandler();
        for (int i = 0; i < 50; i++) {
            String address = handler.route(nodes(), "t").get(0).getAddress();
            assertThat(address).isIn("http://a:1", "http://b:2", "http://c:3");
        }
    }

    @Test
    void shardingBroadcastCoversAllNodes() {
        List<RouteTarget> targets = new ShardingBroadcastHandler().route(nodes(), "t");
        assertThat(targets).hasSize(3);
        for (int i = 0; i < 3; i++) {
            assertThat(targets.get(i).getShardIndex()).isEqualTo(i);
            assertThat(targets.get(i).getShardTotal()).isEqualTo(3);
        }
    }

    @Test
    void consistentHashIsStableForSameTask() {
        ConsistentHashHandler handler = new ConsistentHashHandler();
        String first = handler.route(nodes(), "task-42").get(0).getAddress();
        for (int i = 0; i < 20; i++) {
            assertThat(handler.route(nodes(), "task-42").get(0).getAddress()).isEqualTo(first);
        }
    }

    @Test
    void failoverPicksFirstIdle() {
        ExecutorClient client = mock(ExecutorClient.class);
        when(client.beat("http://a:1")).thenReturn(false);
        when(client.beat("http://b:2")).thenReturn(true);
        List<RouteTarget> targets = new FailoverHandler(client).route(nodes(), "t");
        assertThat(targets.get(0).getAddress()).isEqualTo("http://b:2");
    }

    @Test
    void failoverFallsBackToFirstWhenAllBusy() {
        ExecutorClient client = mock(ExecutorClient.class);
        when(client.beat(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        assertThat(new FailoverHandler(client).route(nodes(), "t").get(0).getAddress()).isEqualTo("http://a:1");
    }

    @Test
    void routerDispatchesByStrategy() {
        ExecutorClient client = mock(ExecutorClient.class);
        ExecutorRouter router = new ExecutorRouter(List.of(
                new FirstHandler(), new LastHandler(), new RoundHandler(), new RandomHandler(),
                new ShardingBroadcastHandler(), new ConsistentHashHandler(), new FailoverHandler(client)));
        assertThat(router.route(RouteStrategy.LAST, nodes(), "t").get(0).getAddress()).isEqualTo("http://c:3");
        assertThat(router.route(RouteStrategy.SHARDING_BROADCAST, nodes(), "t")).hasSize(3);
    }

    @Test
    void routerReturnsEmptyWhenNoNodes() {
        ExecutorRouter router = new ExecutorRouter(List.of(new FirstHandler()));
        assertThat(router.route(RouteStrategy.FIRST, List.of(), "t")).isEmpty();
        assertThat(router.route(RouteStrategy.FIRST, null, "t")).isEmpty();
    }
}