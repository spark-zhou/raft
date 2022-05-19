package com.spark.raft.core.rpc.message;

import com.spark.raft.core.node.NodeId;
import io.netty.channel.Channel;

public class AppendEntriesResultMessage extends AbstractRpcMessage<AppendEntriesResult> {


    public AppendEntriesResultMessage(AppendEntriesResult result, NodeId sourceNodeId, Channel channel) {
        super(result, sourceNodeId, channel);
    }
}
