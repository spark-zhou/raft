package com.spark.raft.core.node.store;

import com.spark.raft.core.node.NodeId;

import javax.annotation.Nullable;

public class MemoryNodeStore implements NodeStore {

    private int term;

    private NodeId votedFor;

    public MemoryNodeStore() {
        this(0,null);
    }

    public MemoryNodeStore(int term, NodeId votedFor) {
        this.term = term;
        this.votedFor = votedFor;
    }

    @Override
    public int getTerm() {
        return this.term;
    }

    @Override
    public void setTerm(int term) {
        this.term = term;
    }

    @Nullable
    @Override
    public NodeId getVotedFor() {
        return this.votedFor;
    }

    @Override
    public void setVotedFor(@Nullable NodeId votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public void close() {

    }
}
