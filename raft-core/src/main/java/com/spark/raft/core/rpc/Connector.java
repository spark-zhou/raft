package com.spark.raft.core.rpc;

import com.spark.raft.core.node.NodeEndpoint;
import com.spark.raft.core.rpc.message.AppendEntriesResult;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;
import com.spark.raft.core.rpc.message.RequestVoteResult;
import com.spark.raft.core.rpc.message.RequestVoteRpc;

import java.util.Collection;

public interface Connector {

    void initialize();

    void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndPoints);

    void replyRequestVote(RequestVoteResult result, NodeEndpoint destinationEndPoint);

    void sendAppendEnrties(AppendEntriesRpc rpc, NodeEndpoint destinationEndPoint);

    void replyAppendEnrties(AppendEntriesResult result, NodeEndpoint destinationEndPoint);



    void resetChannels();
    void close();

}
