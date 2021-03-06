package com.spark.raft.core.node;

import com.google.common.eventbus.EventBus;
import com.spark.raft.core.log.Log;
import com.spark.raft.core.node.store.NodeStore;
import com.spark.raft.core.rpc.Connector;
import com.spark.raft.core.schedule.Scheduler;
import com.spark.raft.core.support.TaskExecutor;

public class NodeContext {

    private NodeId selfId;

    private NodeGroup group;

    private Log log;

    private Connector connector;

    private Scheduler scheduler;

    private EventBus eventBus;

    private TaskExecutor taskExecutor;

    private NodeStore store;

    public NodeId getSelfId() {
        return selfId;
    }

    public void setSelfId(NodeId selfId) {
        this.selfId = selfId;
    }

    public NodeGroup getGroup() {
        return group;
    }

    public void setGroup(NodeGroup group) {
        this.group = group;
    }

    public Connector getConnector() {
        return connector;
    }

    public void setConnector(Connector connector) {
        this.connector = connector;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public NodeStore getStore() {
        return store;
    }

    public void setStore(NodeStore store) {
        this.store = store;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }
}
