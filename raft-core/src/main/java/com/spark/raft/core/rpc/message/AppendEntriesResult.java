package com.spark.raft.core.rpc.message;

public class AppendEntriesResult {

    /**
     * 选举term
     */
    private final int term;

    /**
     * 是否添加成功
     */
    private final boolean success;


    public AppendEntriesResult(int term, boolean success) {
        this.term = term;
        this.success = success;
    }

    public int getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "AppendEntriesResult{" +
                "term=" + term +
                ", success=" + success +
                '}';
    }
}
