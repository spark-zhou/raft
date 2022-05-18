package com.spark.raft.core.rpc.message;

import java.io.Serializable;

/**
 * 选举响应
 */
public class RequestVoteResult implements Serializable {

    /**
     * 选举term
     */
    private final int term;

    /**
     * 是否投票
     */
    private final boolean voteGranted;


    public RequestVoteResult(int term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public String toString() {
        return "RequestVoteResult{" +
                "term=" + term +
                ", voteGranted=" + voteGranted +
                '}';
    }
}
