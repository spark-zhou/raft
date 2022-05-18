package com.spark.raft.core.node.role;

import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.schedule.ElectionTimeout;

public class FollowerNodeRole extends AbstractNodeRole {

    /**
     * 投过票的节点，可为空
     */
    private final NodeId votedFor;

    /**
     * 当前leader节点ID
     */
    private final NodeId leaderId;

    /**
     * 选举超时
     */
    private final ElectionTimeout electionTimeout;

    public FollowerNodeRole(int term, NodeId votedFor, NodeId leaderId, ElectionTimeout electionTimeout) {
        super(RoleName.FOLLOWER, term);
        this.votedFor = votedFor;
        this.leaderId = leaderId;
        this.electionTimeout = electionTimeout;
    }

    @Override
    public void cancelTimeoutOrTask() {
        this.electionTimeout.cancel();
    }

    public NodeId getVotedFor() {
        return votedFor;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    @Override
    public String toString() {
        return "FollowerNodeRole{" +
                "term=" + term +
                ", votedFor=" + votedFor +
                ", leaderId=" + leaderId +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
