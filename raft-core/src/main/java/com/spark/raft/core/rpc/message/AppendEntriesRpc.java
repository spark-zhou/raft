package com.spark.raft.core.rpc.message;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.node.NodeId;

import java.util.Collections;
import java.util.List;

public class AppendEntriesRpc {

    /**
     * 选举term
     */
    private int term;

    private NodeId leaderId;

    /**
     * 前一条日志的索引
     */
    private int prevLogIndex;

    /**
     * 前一条日志的term
     */
    private int prevLogTerm;

    /**
     * 复制的日志条目
     */
    private List<Entry> entries = Collections.emptyList();

    /**
     * leader的commitIndex
     */
    private int leaderCommit;

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public NodeId getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(NodeId leaderId) {
        this.leaderId = leaderId;
    }

    public int getPrevLogIndex() {
        return prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public int getLeaderCommit() {
        return leaderCommit;
    }

    public void setLeaderCommit(int leaderCommit) {
        this.leaderCommit = leaderCommit;
    }

    @Override
    public String toString() {
        return "AppendEntriesRpc{" +
                "term=" + term +
                ", leaderId=" + leaderId +
                ", prevLogIndex=" + prevLogIndex +
                ", prevLogTerm=" + prevLogTerm +
                ", entries=" + entries +
                ", leaderCommit=" + leaderCommit +
                '}';
    }
}
