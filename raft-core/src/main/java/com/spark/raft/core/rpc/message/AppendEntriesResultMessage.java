package com.spark.raft.core.rpc.message;

import com.spark.raft.core.node.NodeId;
import io.netty.channel.Channel;

import javax.annotation.Nonnull;

public class AppendEntriesResultMessage {

    private final AppendEntriesResult result;
    private final NodeId sourceNodeId;
    // TODO remove rpc, just lastEntryIndex required, or move to replicating state?
    private final AppendEntriesRpc rpc;

    public AppendEntriesResultMessage(AppendEntriesResult result, NodeId sourceNodeId,@Nonnull AppendEntriesRpc rpc) {

        this.result = result;
        this.sourceNodeId = sourceNodeId;
        this.rpc = rpc;
    }

    public AppendEntriesResult get() {
        return result;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public AppendEntriesRpc getRpc() {
        return rpc;
    }
}
