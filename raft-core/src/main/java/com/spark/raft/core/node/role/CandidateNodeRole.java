package com.spark.raft.core.node.role;

import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.schedule.ElectionTimeout;

/**
 * CandidateNodeRole
 */
public class CandidateNodeRole extends AbstractNodeRole{

    /**
     * 票数
     */
    private final int votesCount;

    /**
     * 选举超时
     */
    private final ElectionTimeout electionTimeout;



    public CandidateNodeRole(int term, ElectionTimeout electionTimeout) {
        this(term,1,electionTimeout);
    }


    public CandidateNodeRole(int term, int votesCount, ElectionTimeout electionTimeout) {
        super(RoleName.CANDIDATE, term);
        this.votesCount = votesCount;
        this.electionTimeout = electionTimeout;
    }

    @Override
    public NodeId getLeaderId(NodeId selfId) {
        return null;
    }

    @Override
    public RoleState getState() {
//        DefaultRoleState state = new DefaultRoleState(RoleName.CANDIDATE, term);
//        state.setVotesCount(votesCount);
//        return state;
        return null;
    }

    @Override
    protected boolean doStateEquals(AbstractNodeRole role) {
        return false;
    }

    @Override
    public void cancelTimeoutOrTask() {
        electionTimeout.cancel();
    }


    public CandidateNodeRole increaseVotesCount(ElectionTimeout electionTimeout) {
        electionTimeout.cancel();
        return new CandidateNodeRole(this.votesCount + 1,electionTimeout);
    }


    public int getVotesCount() {
        return votesCount;
    }

    @Override
    public String toString() {
        return "CandidateNodeRole{" +
                "term=" + term +
                ", votesCount=" + votesCount +
                ", electionTimeout=" + electionTimeout +
                '}';
    }
}
