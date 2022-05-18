package com.spark.raft.core.rpc.message;

import com.spark.raft.core.node.NodeId;

import java.io.Serializable;

/**
 * 选举RPC请求
 */
public class RequestVoteRpc implements Serializable {

    /**
     * 选举term
     */
    private int term;

    /**
     * 候选者节点ID,候选者自己的ID
     */
    private NodeId candidateId;

    /**
     * 最后一条日志的索引
     */
    private int lastLogIndex = 0;

    /**
     * 最后一条日志的term
     */
    private int lastLogTerm = 0;


    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(NodeId candidateId) {
        this.candidateId = candidateId;
    }

    public int getLastLogIndex() {
        return lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogTerm() {
        return lastLogTerm;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public String toString() {
        return "RequestVoteRpc{" +
                "term=" + term +
                ", candidateId=" + candidateId +
                ", lastLogIndex=" + lastLogIndex +
                ", lastLogTerm=" + lastLogTerm +
                '}';
    }
}
