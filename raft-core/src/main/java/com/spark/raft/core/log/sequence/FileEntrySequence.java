package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.LogDir;
import com.spark.raft.core.log.LogException;
import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class FileEntrySequence extends AbstractEntrySequence {

    private final EntriesFile entriesFile;

    private final EntryIndexFile entryIndexFile;

    private final LinkedList<Entry> pendingEntries = new LinkedList<>();

    private int commitIndex = 0;

    public FileEntrySequence(LogDir logDir, int logIndexOffset) {
        super(logIndexOffset);
        try {
            this.entriesFile = new EntriesFile(logDir.getEntriesFile());
            this.entryIndexFile = new EntryIndexFile(logDir.getEntryOffsetIndexFile());
            initialize();
        } catch (IOException e) {
            throw new LogException("failed to open entries file or entry index file",e);
        }
    }

    public FileEntrySequence(EntriesFile entriesFile, EntryIndexFile entryIndexFile,int logIndexOffset) {
        super(logIndexOffset);
        this.entryIndexFile = entryIndexFile;
        this.entriesFile = entriesFile;
        initialize();
    }

    private void initialize() {
        if (entryIndexFile.isEmpty()) {
            return;
        }

        logIndexOffset = entryIndexFile.getMinEntryIndex();
        nextLogIndex = entryIndexFile.getMaxEntryIndex() + 1;
    }

    @Override
    protected Entry doGetEntry(int index) {

        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index >= firstPendingEntryIndex) {
                return pendingEntries.get(index - firstPendingEntryIndex);
            }
        }

        assert !entryIndexFile.isEmpty();
        return getEntryInFile(index);
    }

    private Entry getEntryInFile(int index) {

        long offset = entryIndexFile.getOffset(index);

        try {
            return entriesFile.loadEntry(offset);
        } catch (IOException e) {
            throw new LogException("failed to load entry " + index,e);
        }
    }

    public EntryMeta getEntryMeta(int index) {

        if (!isEntryPresent(index)) {
            return null;
        }

        if (!pendingEntries.isEmpty()) {
            int firstPendingEntryIndex = pendingEntries.getFirst().getIndex();
            if (index > firstPendingEntryIndex) {
                return pendingEntries.get(index - firstPendingEntryIndex).getMeta();
            }
        }

        return entryIndexFile.get(index).toEntryMeta();
    }

    /**
     * 获取最后一条日志
     * @return
     */
    public Entry getLastEntry() {

        if (isEmpty()) {
            return null;
        }

        if (!pendingEntries.isEmpty()) {
            return pendingEntries.getLast();
        }

        assert !entryIndexFile.isEmpty();
        return getEntryInFile(entryIndexFile.getMaxEntryIndex());
    }

    @Override
    protected List<Entry> doSubList(int fromIndex, int toIndex) {

        List<Entry> result = new ArrayList<>();

        //先从文件中获取
        if (!entryIndexFile.isEmpty() && fromIndex <= entryIndexFile.getMaxEntryIndex()) {
            int maxIndex = Math.min(entryIndexFile.getMaxEntryIndex()+1,toIndex);
            for (int i = fromIndex; i < maxIndex; i++) {
                result.add(getEntryInFile(i));
            }
        }

        //再从缓存中获取
        if (!pendingEntries.isEmpty() && toIndex > pendingEntries.getFirst().getIndex()) {
            Iterator<Entry> iterator = pendingEntries.iterator();
            Entry entry;
            int index;
            while (iterator.hasNext()) {
                entry = iterator.next();
                index = entry.getIndex();
                if (index >= toIndex) {
                    break;
                }

                if (index >= fromIndex) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    @Override
    protected void doAppend(Entry entry) {

        pendingEntries.add(entry);
    }

    @Override
    protected void doRemoveAfter(int index) {

        if (!pendingEntries.isEmpty() && index >= pendingEntries.getFirst().getIndex() -1) {
            for (int i = index + 1; i <= doGetLastLogIndex(); i++) {
                pendingEntries.removeLast();
            }
            nextLogIndex = index + 1;
            return;
        }

        try {
            if (index >= doGetFirstLogIndex()) {
                pendingEntries.clear();
                entriesFile.truncate(entryIndexFile.getOffset(index + 1));
                entryIndexFile.removeAfter(index);
                nextLogIndex = index + 1;
                commitIndex = index;
            } else {
                pendingEntries.clear();
                entriesFile.clear();
                entryIndexFile.clear();
                nextLogIndex = logIndexOffset;
                commitIndex = logIndexOffset - 1;
            }
        }catch (IOException e) {
            throw new LogException(e);
        }
    }

    /**
     * commit的主要目的是把缓存中的日志条目数据保存到文件中
     * @param index
     */
    @Override
    public void commit(int index) {
        if (index < commitIndex) {
            throw new IllegalArgumentException("commit index < " + commitIndex);
        }

        if (index == commitIndex) {
            return;
        }

        if (!entryIndexFile.isEmpty() && index <= entryIndexFile.getMaxEntryIndex()) {
            commitIndex = index;
            return;
        }

        if (pendingEntries.isEmpty() || pendingEntries.getFirst().getIndex() > index || pendingEntries.getLast().getIndex() < index) {
            throw new IllegalArgumentException("no entry to commit or commit index exceed.");
        }

        long offset;
        Entry entry = null;
        try {
            for (int i = pendingEntries.getFirst().getIndex(); i <= index; i++) {
                entry = pendingEntries.removeFirst();
                offset = entriesFile.appendEntry(entry);
                entryIndexFile.appendEntryIndex(i,offset,entry.getKind(),entry.getTerm());
                commitIndex = i;
            }
        }catch (IOException e) {
            throw new LogException("failed to commit entry " + entry,e);
        }
    }

    @Override
    public int getCommitIndex() {
        return this.commitIndex;
    }

    @Override
    public void close() {
        try {
            entriesFile.close();
            entryIndexFile.close();
        } catch (IOException e) {
            throw new LogException("failed to close", e);
        }

    }
}
