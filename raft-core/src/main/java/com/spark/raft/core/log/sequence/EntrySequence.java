package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryMeta;

import java.util.List;

public interface EntrySequence {

    boolean isEmpty();

    int getFirstLogIndex();

    int getLastLogIndex();

    int getNextLogIndex();

    List<Entry> subList(int fromIndex);

    List<Entry> subList(int fromIndex,int toIndex);

    /**
     * 检查某个条目是否存在
     * @param index
     * @return
     */
    boolean isEntryPresent(int index);

    /**
     * 获取某条目的元信息
     * @param index
     * @return
     */
    EntryMeta getEntryMeta(int index);

    Entry getEntry(int index);

    Entry getLastEntry();

    void append(Entry entry);

    void append(List<Entry> entries);

    /**
     * 推荐commitIndex
     * @param index
     */
    void commit(int index);

    int getCommitIndex();

    /**
     * 移出某个条目之后的日志条目
     * @param index
     */
    void removeAfter(int index);

    void close();
}
