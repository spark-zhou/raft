package com.spark.raft.core.rpc;

import com.spark.raft.core.node.NodeEndpoint;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.message.AppendEntriesResult;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;
import com.spark.raft.core.rpc.message.RequestVoteResult;
import com.spark.raft.core.rpc.message.RequestVoteRpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MockConnector implements Connector {

    private LinkedList<Message> messages = new LinkedList<>();

    @Override
    public void initialize() {
    }

    @Override
    public void sendRequestVote(RequestVoteRpc rpc, Collection<NodeEndpoint> destinationEndPoints) {

        Message message = new Message();
        message.rpc = rpc;
        messages.add(message);
    }

    @Override
    public void replyRequestVote(RequestVoteResult result, NodeEndpoint destinationEndPoint) {

        Message message = new Message();
        message.result = result;
        message.setDestinationNodeId(destinationEndPoint.getId());
        messages.add(message);
    }

    @Override
    public void sendAppendEnrties(AppendEntriesRpc rpc, NodeEndpoint destinationEndPoint) {

        Message message = new Message();
        message.rpc = rpc;
        message.setDestinationNodeId(destinationEndPoint.getId());
        messages.add(message);
    }

    @Override
    public void replyAppendEnrties(AppendEntriesResult result, NodeEndpoint destinationEndPoint) {

        Message message = new Message();
        message.result = result;
        message.setDestinationNodeId(destinationEndPoint.getId());
        messages.add(message);
    }

    @Override
    public void resetChannels() {

    }

    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    private Message getLastMessageOrDefault() {
        return messages.isEmpty() ? new Message() : messages.getLast();
    }

    public Object getRpc() {
        return getLastMessageOrDefault().rpc;
    }

    public Object getResult() {
        return getLastMessageOrDefault().result;
    }

    public NodeId getDestinationNodeId() {
        return getLastMessageOrDefault().destinationNodeId;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clearMessage() {
        messages.clear();
    }

    @Override
    public void close() {
    }

    public static class Message {
        private Object rpc;

        private NodeId destinationNodeId;

        private Object result;

        public Object getRpc() {
            return rpc;
        }

        public void setRpc(Object rpc) {
            this.rpc = rpc;
        }

        public NodeId getDestinationNodeId() {
            return destinationNodeId;
        }

        public void setDestinationNodeId(NodeId destinationNodeId) {
            this.destinationNodeId = destinationNodeId;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "rpc=" + rpc +
                    ", destinationNodeId=" + destinationNodeId +
                    ", result=" + result +
                    '}';
        }
    }
}
