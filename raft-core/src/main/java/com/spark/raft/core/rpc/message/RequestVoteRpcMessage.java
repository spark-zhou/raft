package com.spark.raft.core.rpc.message;

import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.Channel;

public class RequestVoteRpcMessage extends AbstractRpcMessage<RequestVoteRpc> {


    public RequestVoteRpcMessage(RequestVoteRpc rpc, NodeId sourceNodeId, Channel channel) {
        super(rpc, sourceNodeId, channel);
    }
}
