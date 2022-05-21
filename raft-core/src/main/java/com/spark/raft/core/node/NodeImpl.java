package com.spark.raft.core.node;

import com.google.common.eventbus.Subscribe;
import com.spark.raft.core.log.entry.EntryMeta;
import com.spark.raft.core.node.role.*;
import com.spark.raft.core.node.store.NodeStore;
import com.spark.raft.core.rpc.message.*;
import com.spark.raft.core.schedule.ElectionTimeout;
import com.spark.raft.core.schedule.LogReplicationTask;
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

    @Subscribe
    public void onRecieveAppendEntriesRpc(AppendEntriesRpcMessage message) {

        context.getTaskExecutor().submit(() -> context.getConnector().replyAppendEnrties(
                doProcessAppendEntriesRpc(message),
                context.getGroup().findMember(message.getSourceNodeId()).getEndpoint()
        ));
    }

    @Subscribe
    public void onReceiveAppendEntriesResult(AppendEntriesResultMessage resultMessage) {

        context.getTaskExecutor().submit(() -> doProcessAppendEntriesResult(resultMessage));
    }

    private void doProcessAppendEntriesResult(AppendEntriesResultMessage resultMessage) {
        AppendEntriesResult result = resultMessage.get();

        if (result.getTerm() > role.getTerm()) {
            becomeFollower(result.getTerm(),null,null,true);
            return;
        }

        if (role.getName() != RoleName.LEADER) {
            logger.warn("received append entries result from node {} but current node is not leader, ignore.",resultMessage.getSourceNodeId());
            return;
        }

        NodeId sourceNodeId = resultMessage.getSourceNodeId();
        GroupMember member = context.getGroup().getMember(sourceNodeId);

        if (member == null) {
            logger.warn("unexpected append entries result from node {}, node maybe removed.",sourceNodeId);
            return;
        }

        AppendEntriesRpc rpc = resultMessage.getRpc();
        if (result.isSuccess()) {
            if (member.advanceReplicatingState(rpc.getLastEntryIndex())) {
                context.getLog().advanceCommitIndex(context.getGroup().getMatchIndexOfMajor(),role.getTerm());
            }
        } else {
            if (!member.backOffNextIndex()) {
                logger.warn("cannot back off next index more,node {}",sourceNodeId);
            }
        }
    }

    private AppendEntriesResult doProcessAppendEntriesRpc(AppendEntriesRpcMessage message) {

        AppendEntriesRpc rpc = message.get();

        if (rpc.getTerm() < role.getTerm()) {
            return new AppendEntriesResult(role.getTerm(),false);
        }

        if (rpc.getTerm() > role.getTerm()) {
            becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(),true);
            return new AppendEntriesResult(rpc.getTerm(), appendEntries(rpc));
        }

        assert rpc.getTerm() == role.getTerm();
        switch (role.getName()) {
            case FOLLOWER:
                becomeFollower(rpc.getTerm(),((FollowerNodeRole)role).getVotedFor(),rpc.getLeaderId(),true);
                return new AppendEntriesResult(rpc.getTerm(), appendEntries(rpc));
            case CANDIDATE:
                becomeFollower(rpc.getTerm(), null, rpc.getLeaderId(),true);
                return new AppendEntriesResult(rpc.getTerm(), appendEntries(rpc));
            case LEADER:
                logger.warn("receive append entries rpc from another leader {}, ignore..",rpc.getLeaderId());
                return new AppendEntriesResult(rpc.getTerm(), false);
            default:
                throw new IllegalStateException("unexpected node role [" + role.getName().name() +"]");
        }
    }

    private boolean appendEntries(AppendEntriesRpc rpc) {

        boolean result = context.getLog().appendEntriesFromLeader(rpc.getPrevLogIndex(),rpc.getPrevLogTerm(),rpc.getEntries());

        if (result) {
            context.getLog().advanceCommitIndex(Math.min(rpc.getLeaderCommit(),context.getLog().getLastEntryMeta().getIndex()),rpc.getTerm());
        }
        return result;
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
            voteForCandidate = !context.getLog().isNewerThan(rpc.getLastLogIndex(), rpc.getLastLogTerm());
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

        if (result.getTerm() > role.getTerm()) {
            becomeFollower(result.getTerm(), null,null,true);
            return;
        }

        if (role.getName() != RoleName.CANDIDATE) {
            logger.info("receive request vote result and current role is not candidate, ignore...");
            return;
        }

        if (result.getTerm() < role.getTerm() || !result.isVoteGranted()) {
            return;
        }

        int currentVotesCount = ((CandidateNodeRole) role).getVotesCount() + 1;

        int countOfMajor = context.getGroup().getCountOfMajor();
        logger.info("votes count {}, node count {}",currentVotesCount,countOfMajor);

        role.cancelTimeoutOrTask();

        if (currentVotesCount > countOfMajor / 2) {
            logger.info("become leader, term = {}",role.getTerm());
            changeToRole(new LeaderNodeRole(role.getTerm(),scheduleLogReplicationTask()));
        } else {
            changeToRole(new CandidateNodeRole(role.getTerm(),currentVotesCount,scheduleElectionTimeout()));
        }

    }

    private LogReplicationTask scheduleLogReplicationTask() {
        return context.getScheduler().scheduleLogReplicationTask(this::replicateLog);
    }

    public void replicateLog() {
        context.getTaskExecutor().submit(this::doReplicateLog);
    }

    void doReplicateLog() {

        logger.info("replicate log");

        for (GroupMember member : context.getGroup().listReplicationTarget()) {
            sendReplicateLog(member,-1);
        }
    }

    private void sendReplicateLog(GroupMember member, int maxEntries) {

//        AppendEntriesRpc rpc = new AppendEntriesRpc();
//        rpc.setTerm(role.getTerm());
//        rpc.setLeaderId(context.getSelfId());
//        rpc.setPrevLogIndex(0);
//        rpc.setPrevLogTerm(0);
//        rpc.setLeaderCommit(0);
        AppendEntriesRpc rpc = context.getLog().createAppendEntriesRpc(role.getTerm(),context.getSelfId(),member.getNextIndex(),maxEntries);
        context.getConnector().sendAppendEnrties(rpc,member.getEndpoint());
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

        EntryMeta meta = context.getLog().getLastEntryMeta();

        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(newTerm);
        rpc.setCandidateId(context.getSelfId());
        rpc.setLastLogIndex(meta.getIndex());
        rpc.setLastLogTerm(meta.getTerm());
        context.getConnector().sendRequestVote(rpc,context.getGroup().listEndpointOfMajorExceptSelf());
    }

    NodeContext getContext() {
        return this.context;
    }

    AbstractNodeRole getRole() {
        return this.role;
    }
}
