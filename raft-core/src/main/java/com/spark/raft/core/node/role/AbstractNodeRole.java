package com.spark.raft.core.node.role;

import com.spark.raft.core.node.NodeId;

public abstract class AbstractNodeRole {

    private final RoleName name;

    protected final int term;

    public AbstractNodeRole(RoleName name, int term) {
        this.name = name;
        this.term = term;
    }

    public abstract NodeId getLeaderId(NodeId selfId);

    public abstract RoleState getState();

    public boolean stateEquals(AbstractNodeRole that) {
        if (this.name != that.name || this.term != that.term) {
            return false;
        }
        return doStateEquals(that);
    }

    protected abstract boolean doStateEquals(AbstractNodeRole role);

    /**
     * 取消超时或者定时任务
     */
    public abstract void cancelTimeoutOrTask();

    public RoleName getName() {
        return name;
    }

    public int getTerm() {
        return term;
    }
}
