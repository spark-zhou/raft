package com.spark.raft.core.rpc.message;

import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.Channel;

public class AppendEntriesRpcMessage extends AbstractRpcMessage<AppendEntriesRpc> {


    public AppendEntriesRpcMessage(AppendEntriesRpc rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
