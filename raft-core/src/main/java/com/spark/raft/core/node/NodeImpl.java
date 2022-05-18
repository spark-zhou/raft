package com.spark.raft.core.node;

import com.spark.raft.core.node.role.AbstractNodeRole;
import com.spark.raft.core.node.role.CandidateNodeRole;
import com.spark.raft.core.node.role.FollowerNodeRole;
import com.spark.raft.core.node.role.RoleName;
import com.spark.raft.core.node.store.NodeStore;
import com.spark.raft.core.rpc.message.RequestVoteRpc;
import com.spark.raft.core.schedule.ElectionTimeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
