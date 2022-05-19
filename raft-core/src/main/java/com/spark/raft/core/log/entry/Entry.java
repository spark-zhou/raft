package com.spark.raft.core.log.entry;

public interface Entry {

    /**
     * 空操作日志类型
     */
    int KIND_NO_OP = 0;

    /**
     * 普通操作日志类型
     */
    int KIND_GENERAL = 1;

    /**
     * 添加节点日志类型
     */
    int KIND_ADD_NODE = 3;

    /**
     * 删除节点日志类型
     */
    int KIND_REMOVE_NODE = 4;

    int getKind();

    int getIndex();

    int getTerm();

    EntryMeta getMeta();

    byte[] getCommandBytes();
}
