package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.entry.Entry;

import java.util.List;

public class FileEntrySequence extends AbstractEntrySequence {

    public FileEntrySequence(int logIndexOffset) {
        super(logIndexOffset);
    }

    @Override
    protected Entry doGetEntry(int index) {
        return null;
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {
        return null;
    }

    @Override
    protected void doAppend(Entry entry) {

    }

    @Override
    protected void doRemoveAfter(int index) {

    }

    @Override
    public void commit(int index) {

    }

    @Override
    public int getCommitIndex() {
        return 0;
    }

    @Override
    public void close() {

    }
}
