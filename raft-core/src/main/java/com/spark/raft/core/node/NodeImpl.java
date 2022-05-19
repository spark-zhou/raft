package com.spark.raft.core.node;

import com.google.common.eventbus.Subscribe;
import com.spark.raft.core.node.role.AbstractNodeRole;
import com.spark.raft.core.node.role.CandidateNodeRole;
import com.spark.raft.core.node.role.FollowerNodeRole;
import com.spark.raft.core.node.role.RoleName;
import com.spark.raft.core.node.store.NodeStore;
import com.spark.raft.core.rpc.message.RequestVoteResult;
import com.spark.raft.core.rpc.message.RequestVoteRpc;
import com.spark.raft.core.rpc.message.RequestVoteRpcMessage;
import com.spark.raft.core.schedule.ElectionTimeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class NodeImpl implements Node {

    private static final Logger logger = LoggerFactory.getLogger(NodeImpl.class);

    private final NodeContext context;

    private boolean started;

    private AbstractNodeRole role;

    NodeImpl(NodeContext context) {
        this.context = context;
    }

    @Override
    public synchronized void start() {

        if (started) {
            return;
        }

        context.getEventBus().register(this);

        context.getConnector().initialize();

        NodeStore store = context.getStore();
        changeToRole(new FollowerNodeRole(store.getTerm(),store.getVotedFor(),null,scheduleElectionTimeout()));

    }

    @Subscribe
    public void onReceiveRequestVoteRpc(RequestVoteRpcMessage message) {
        NodeEndpoint srcEndpoint = context.getGroup().findMember(message.getSourceNodeId()).getEndpoint();
        context.getTaskExecutor().submit(() -> context.getConnector().replyRequestVote(doProcessRequestVoteRpc(message), srcEndpoint));
    }

    @Subscribe
    public void onReceiveRequestVoteResult(RequestVoteResult result) {
        context.getTaskExecutor().submit(() -> doProcessRequestVoteResult(result));
    }

    private RequestVoteResult doProcessRequestVoteRpc(RequestVoteRpcMessage message) {

        if (!context.getGroup().isMemberOfMajor(message.getSourceNodeId())) {
            logger.warn("receive request vote rpc from node {} which is not major node, ignore", message.getSourceNodeId());
            return new RequestVoteResult(role.getTerm(), false);
        }

        // reply current term if result's term is smaller than current one
        RequestVoteRpc rpc = message.get();
        if (rpc.getTerm() < role.getTerm()) {
            logger.debug("term from rpc < current term, don't vote ({} < {})", rpc.getTerm(), role.getTerm());
            return new RequestVoteResult(role.getTerm(), false);
        }

        // step down if result's term is larger than current term
        boolean voteForCandidate = true;
        if (rpc.getTerm() > role.getTerm()) {
//            boolean voteForCandidate = !context.log().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm());
            becomeFollower(rpc.getTerm(), (voteForCandidate ? rpc.getCandidateId() : null), null, true);
            return new RequestVoteResult(rpc.getTerm(), voteForCandidate);
        }

        switch (role.getName()) {
            case FOLLOWER:
                    FollowerNodeRole follower = (FollowerNodeRole) role;
                    NodeId votedFor = follower.getVotedFor();

                    if ((votedFor == null && voteForCandidate) || Objects.equals(votedFor,rpc.getCandidateId())) {
                        becomeFollower(role.getTerm(),rpc.getCandidateId(),null,true);
                        return new RequestVoteResult(rpc.getTerm(),true);
                    }
                    return new RequestVoteResult(role.getTerm(),false);
            case CANDIDATE:
            case LEADER:
                return new RequestVoteResult(role.getTerm(),false);
            default:
                throw  new IllegalStateException("unexpected node role [" + role.getName() + "]");
        }
    }

    private void doProcessRequestVoteResult(RequestVoteResult result) {

    }

    private void becomeFollower(int term,NodeId votedFor, NodeId leaderId,boolean scheduleElectionTimeout) {

        role.cancelTimeoutOrTask();
        if (leaderId != null && !leaderId.equals(role.getLeaderId(context.getSelfId()))) {
            logger.info("current leader is {},term {}",leaderId,term);
        }

        ElectionTimeout electionTimeout = scheduleElectionTimeout ? scheduleElectionTimeout() : ElectionTimeout.NONE;
        changeToRole(new FollowerNodeRole(term,votedFor,leaderId,electionTimeout));
    }

    @Override
    public void stop() throws InterruptedException {

        if (!started) {
            throw new IllegalStateException("node not started.");
        }

        context.getScheduler().stop();
        context.getConnector().close();
        context.getTaskExecutor().shutdown();
        started = false;
    }

    void electionTimeout() {

        context.getTaskExecutor().submit(this::doProcessElectionTimeout);
    }

    private void changeToRole(AbstractNodeRole newRole) {

        logger.debug("node {}, role state changed -> {}",context.getSelfId(),newRole);
        NodeStore store = context.getStore();
        store.setTerm(newRole.getTerm());

        if (newRole.getName() == RoleName.FOLLOWER) {
            store.setVotedFor(((FollowerNodeRole)newRole).getVotedFor());
        }

        this.role = newRole;
    }

    private ElectionTimeout scheduleElectionTimeout() {
        return context.getScheduler().scheduleElectionTimeout(this::electionTimeout);
    }

    private void doProcessElectionTimeout() {

        if (role.getName() == RoleName.LEADER) {
            logger.warn("node {},current role is leader, ignore ecltion",context.getSelfId());
            return;
        }

        int newTerm = role.getTerm() + 1;
        role.cancelTimeoutOrTask();
        logger.info("start election");

        changeToRole(new CandidateNodeRole(newTerm,scheduleElectionTimeout()));

        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(newTerm);
        rpc.setCandidateId(context.getSelfId());
        rpc.setLastLogIndex(0);
        rpc.setLastLogTerm(0);
        context.getConnector().sendRequestVote(rpc,context.getGroup().listEndpointOfMajorExceptSelf());
    }
}
