package com.spark.raft.core.node;

import com.google.common.eventbus.EventBus;
import com.spark.raft.core.node.config.NodeConfig;
import com.spark.raft.core.node.store.MemoryNodeStore;
import com.spark.raft.core.rpc.Connector;
import com.spark.raft.core.schedule.DefaultScheduler;
import com.spark.raft.core.schedule.Scheduler;
import com.spark.raft.core.support.SingleThreadTaskExecutor;
import com.spark.raft.core.support.TaskExecutor;

import java.util.Collection;
import java.util.Collections;

public class NodeBuilder {

    private final NodeGroup group;

    private final NodeId selfId;

    private final EventBus eventBus;

    private Scheduler scheduler = null;

    private Connector connector = null;

    private TaskExecutor taskExecutor = null;

    public NodeBuilder(NodeEndpoint endpoint) {
        this(Collections.singletonList(endpoint),endpoint.getId());
    }

    public NodeBuilder(Collection<NodeEndpoint> endpoints,NodeId selfId) {

        this.group = new NodeGroup(endpoints,selfId);
        this.selfId = selfId;
        this.eventBus = new EventBus(selfId.getValue());
    }


    NodeBuilder setConnector(Connector connector) {
        this.connector = connector;
        return this;
    }

    NodeBuilder setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    NodeBuilder setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public Node build() {
        return new NodeImpl(buildContext());
    }

    private NodeContext buildContext() {

        NodeContext context = new NodeContext();

        context.setGroup(group);
        context.setSelfId(selfId);
        context.setEventBus(eventBus);
        context.setScheduler(scheduler != null ? scheduler : new DefaultScheduler(new NodeConfig()));
        context.setConnector(connector);
        context.setTaskExecutor(taskExecutor != null ? taskExecutor : new SingleThreadTaskExecutor("node"));
        context.setStore(new MemoryNodeStore());
        return context;
    }
}
