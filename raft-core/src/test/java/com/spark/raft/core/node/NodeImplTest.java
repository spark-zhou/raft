package com.spark.raft.core.node;

import com.spark.raft.core.node.role.CandidateNodeRole;
import com.spark.raft.core.node.role.FollowerNodeRole;
import com.spark.raft.core.node.role.LeaderNodeRole;
import com.spark.raft.core.node.role.RoleName;
import com.spark.raft.core.rpc.MockConnector;
import com.spark.raft.core.rpc.message.*;
import com.spark.raft.core.schedule.NullScheduler;
import com.spark.raft.core.support.DirectTaskExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeImplTest {

    private NodeBuilder newNodeBuilder(NodeId selfId, NodeEndpoint...endpoints) {
        return new NodeBuilder(Arrays.asList(endpoints),selfId)
                .setConnector(new MockConnector())
                .setScheduler(new NullScheduler())
                .setTaskExecutor(new DirectTaskExecutor());
    }

    /**
     * 测试节点启动，初始化为Follower角色
     * 在超时时间内，如果没有收到选举心跳消息，就发起选举超时事件，升级为Candidate角色进行选举
     */
    @Test
    public void testStart() {
        NodeImpl node = (NodeImpl) newNodeBuilder(NodeId.of("A"),new NodeEndpoint("A","localhost",2333)).build();
        node.start();
        Assert.assertEquals(RoleName.FOLLOWER,node.getRole().getName());
        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(0,role.getTerm());
    }

    /**
     * 选举事件进行，发送选举RequestVote请求
     */
    @Test
    public void testElctionTimeoutWhenFollower() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();
        node.electionTimeout();

        CandidateNodeRole role = (CandidateNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());
        Assert.assertEquals(1,role.getVotesCount());

        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();

        RequestVoteRpc rpc = (RequestVoteRpc) mockConnector.getRpc();
        Assert.assertEquals(1,rpc.getTerm());
        Assert.assertEquals(NodeId.of("A"),rpc.getCandidateId());
        Assert.assertEquals(0,rpc.getLastLogIndex());
        Assert.assertEquals(0,rpc.getLastLogTerm());
    }

    /**
     * follower节点收到其他节点的RequestVote消息，投票并设置自己的votedFor为消息来源节点的ID
     */
    @Test
    public void testOnReceiveRequestVoteRpcFollower() {

        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();

        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(1);
        rpc.setCandidateId(NodeId.of("C"));
        rpc.setLastLogTerm(0);
        rpc.setLastLogIndex(0);
        node.onReceiveRequestVoteRpc(new RequestVoteRpcMessage(rpc,NodeId.of("C"),null));

        MockConnector connector = (MockConnector) node.getContext().getConnector();

        RequestVoteResult result = (RequestVoteResult) connector.getResult();
        Assert.assertEquals(1,result.getTerm());
        Assert.assertTrue(result.isVoteGranted());
        Assert.assertEquals(NodeId.of("C"),((FollowerNodeRole)node.getRole()).getVotedFor());
    }


    /**
     * 测试场景：Candidat角色收到投票响应，并成为leader角色的场景
     */
    @Test
    public void testOnReceiveRequestVoteResult() {

        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();
        node.electionTimeout();

        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));
        Assert.assertEquals(RoleName.LEADER,node.getRole().getName());

        LeaderNodeRole role = (LeaderNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());

    }

    /**
     * 场景：测试A节点成为leader之后，向B,C节点发送消息
     */
    @Test
    public void testReplicateLog() {

        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();
        node.electionTimeout();

        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));
        node.replicateLog();

        MockConnector mockConnector = (MockConnector) node.getContext().getConnector();

        Assert.assertEquals(3,mockConnector.getMessageCount());

        List<MockConnector.Message> messageList = mockConnector.getMessages();

        Set<NodeId> destinationNodeIds = messageList.subList(1,3).stream().map(MockConnector.Message::getDestinationNodeId).collect(Collectors.toSet());

        Assert.assertEquals(2,destinationNodeIds.size());
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("B")));
        Assert.assertTrue(destinationNodeIds.contains(NodeId.of("C")));

        AppendEntriesRpc rpc = (AppendEntriesRpc) messageList.get(2).getRpc();
        Assert.assertEquals(1,rpc.getTerm());
    }

    /**
     * A节点为Follower节点，收到Leader B的心跳信息，设置自己的term和leaderId以及回复OK
     */
    @Test
    public void testOnReceiveAppendEntriesRpcFollower() {

        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(1);
        rpc.setLeaderId(NodeId.of("B"));
        node.onRecieveAppendEntriesRpc(new AppendEntriesRpcMessage(rpc,NodeId.of("B"),null));

        MockConnector connector = (MockConnector) node.getContext().getConnector();

        AppendEntriesResult result = (AppendEntriesResult) connector.getResult();

        Assert.assertEquals(1,result.getTerm());
        Assert.assertTrue(result.isSuccess());

        FollowerNodeRole role = (FollowerNodeRole) node.getRole();
        Assert.assertEquals(1,role.getTerm());
        Assert.assertEquals(NodeId.of("B"),role.getLeaderId());
    }

    @Test
    public void testOnReceiveAppendEntriesNormal() {
        NodeImpl node = (NodeImpl) newNodeBuilder(
                NodeId.of("A"),
                new NodeEndpoint("A","localhost",2333),
                new NodeEndpoint("B","localhost",2334),
                new NodeEndpoint("C","localhost",2335)
        ).build();

        node.start();
        node.electionTimeout();

        node.onReceiveRequestVoteResult(new RequestVoteResult(1,true));

        node.replicateLog();
        node.onReceiveAppendEntriesResult(new AppendEntriesResultMessage(
                new AppendEntriesResult(1,true),
                NodeId.of("B"),
                null));
    }
}
