package com.spark.raft.core.log;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryMeta;
import com.spark.raft.core.log.entry.GeneralEntry;
import com.spark.raft.core.log.entry.NoOpEntry;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;

import java.util.List;

public interface Log {

    int ALL_ENTRIES = -1;

    /**
     * 获取最后一条日志的元信息，选举开始、发送消息时
     * @return
     */
    EntryMeta getLastEntryMeta();

    /**
     * 创建AppendEntries消息，用于leader向follower发送日志复制消息
     * @param term
     * @param selfId
     * @param nextIndex
     * @param maxEntries
     * @return
     */
    AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries);

    /**
     * 获取下一条日志的索引
     * @return
     */
    int getNextIndex();

    /**
     * 获取当前的commitIndex
     * @return
     */
    int getCommitIndex();

    /**
     * 判断对象的lastLogIndex和lastLogTerm是否比自己新
     * @param lastLogIndex
     * @param lastLogTerm
     * @return
     */
    boolean isNewerThan(int lastLogIndex, int lastLogTerm);

    /**
     * 增加一个NO-OP日志
     * @param term
     * @return
     */
    NoOpEntry appendEntry(int term);

    /**
     * 增加一条普通日志
     * @param term
     * @param command
     * @return
     */
    GeneralEntry appendEntry(int term, byte[] command);

    /**
     * 追加来自Leader的日志条目，收到来自leader服务器的日志复制请求时调用
     * @param prevLogIndex
     * @param prevLogTerm
     * @param entries
     * @return
     */
    boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> entries);

    /**
     * 推进 commitIndex，收到来自leader服务器的日志复制请求时调用
     * @param newCommitIndex
     * @param currentTerm
     */
    void advanceCommitIndex(int newCommitIndex, int currentTerm);

    /**
     * 关闭
     */
    void close();
}
