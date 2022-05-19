package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryMeta;

import java.util.Collections;
import java.util.List;

public abstract class AbstractEntrySequence implements EntrySequence {

    int logIndexOffset;

    int nextLogIndex;

    public AbstractEntrySequence(int logIndexOffset) {
        this.logIndexOffset = logIndexOffset;
        this.nextLogIndex = logIndexOffset;
    }

    public boolean isEmpty() {
        return logIndexOffset == nextLogIndex;
    }

    public int getFirstLogIndex() {
        if (isEmpty()) {
            throw new EmptySequenceException();
        }

        return doGetFirstLogIndex();
    }

    int doGetFirstLogIndex() {
        return logIndexOffset;
    }

    public int getLastLogIndex() {
        if(isEmpty()) {
            throw new EmptySequenceException();
        }

        return doGetLastLogIndex();
    }

    int doGetLastLogIndex() {
        return nextLogIndex - 1;
    }

    public int getNextLogIndex() {
        return nextLogIndex;
    }

    public boolean isEntryPresent(int index) {
        return !isEmpty() && index >= doGetFirstLogIndex() && index <= doGetLastLogIndex();
    }

    public Entry getEntry(int index) {
        if (!isEntryPresent(index)) {
            return null;
        }

        return doGetEntry(index);
    }


    public EntryMeta getEntryMeta(int index) {
        Entry entry = getEntry(index);
        return entry != null ? entry.getMeta() : null;
    }

    public Entry getLastEntry() {
        return isEmpty() ? null : doGetEntry(doGetLastLogIndex());
    }

    /**
     * 从具体的存储中读取日志条目的逻辑
     * @param index
     * @return
     */
    protected abstract Entry doGetEntry(int index);


    public List<Entry> subList(int fromIndex) {

        if (isEmpty() || fromIndex > doGetLastLogIndex()) {
            return Collections.emptyList();
        }

        return subList(Math.max(fromIndex,doGetFirstLogIndex()),nextLogIndex);
    }

    public List<Entry> subList(int fromIndex,int toIndex) {

        if (isEmpty()) {
            throw new EmptySequenceException();
        }

        if (fromIndex < doGetFirstLogIndex() || toIndex > doGetLastLogIndex() + 1 || fromIndex > toIndex) {
            throw new IllegalArgumentException("illegal from index " + fromIndex + " or to index " + toIndex);
        }

        return doSubList(fromIndex,toIndex);
    }

    /**
     * 从具体的存储中实现子视图
     * @param fromIndex
     * @param toIndex
     * @return
     */
    protected  abstract List<Entry> doSubList(int fromIndex, int toIndex);

    public void append(List<Entry> entries) {
        for (Entry entry: entries) {
            append(entry);
        }
    }

    public void append(Entry entry) {
        if (entry.getIndex() != nextLogIndex) {
            throw new IllegalArgumentException("entry index must be " + nextLogIndex);
        }

        doAppend(entry);
        nextLogIndex++;
    }

    /**
     * 由具体的存储实现追加日志
     * @param entry
     */
    protected abstract void doAppend(Entry entry);

    public void removeAfter(int index) {
        if (isEmpty() || index >= doGetLastLogIndex()) {
            return;
        }

        doRemoveAfter(index);
    }

    /**
     * 由具体的存储实现移出日志条目
     * @param index
     */
    protected abstract void doRemoveAfter(int index);
}
