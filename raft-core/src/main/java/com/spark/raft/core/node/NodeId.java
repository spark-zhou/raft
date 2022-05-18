package com.spark.raft.core.node;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

public class NodeId implements Serializable {

    private final String value;

    public NodeId(@Nonnull String value) {
        Preconditions.checkNotNull(value);
        this.value = value;
    }

    public static NodeId of(@Nonnull String value) {
        return new NodeId(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeId nodeId = (NodeId) o;
        return value.equals(nodeId.value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return this.value;
    }

}
