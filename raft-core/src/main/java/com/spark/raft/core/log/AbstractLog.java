package com.spark.raft.core.log;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryMeta;
import com.spark.raft.core.log.entry.GeneralEntry;
import com.spark.raft.core.log.entry.NoOpEntry;
import com.spark.raft.core.log.sequence.EntrySequence;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class AbstractLog implements Log {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLog.class);

    protected EntrySequence entrySequence;

    protected int commitIndex = 0;

    public EntryMeta getLastEntryMeta() {
        if (entrySequence.isEmpty()) {
            return new EntryMeta(Entry.KIND_NO_OP,0,0);
        }
        return entrySequence.getLastEntry().getMeta();
    }

    /**
     * 创建AppendEntries消息
     * @param term
     * @param selfId
     * @param nextIndex
     * @param maxEntries
     * @return
     */
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfId, int nextIndex, int maxEntries) {

        int nextLogIndex = entrySequence.getNextLogIndex();
        if (nextIndex > nextLogIndex) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfId);
        rpc.setLeaderCommit(commitIndex);
        Entry entry = entrySequence.getEntry(nextIndex - 1);

        if (entry != null) {
            rpc.setPrevLogIndex(entry.getIndex());
            rpc.setPrevLogTerm(entry.getTerm());
        }

        rpc.setLastEntryIndex(nextIndex);

        if (!entrySequence.isEmpty()) {
            int maxIndex = (maxEntries == ALL_ENTRIES ? nextLogIndex : Math.min(nextLogIndex,nextIndex + maxEntries));
            rpc.setEntries(entrySequence.subList(nextIndex,maxIndex));
            rpc.setLastEntryIndex(maxIndex);
        }

        return rpc;
    }

    /**
     * 比较是否最新，用于投票检查
     * @param lastLogIndex
     * @param lastLogTerm
     * @return
     */
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {

        EntryMeta meta = getLastEntryMeta();
        logger.info("last entry ({},{}),candidate ({},{})",meta.getIndex(),meta.getTerm(),lastLogIndex,lastLogTerm);
        return meta.getTerm() > lastLogTerm || meta.getIndex() > lastLogIndex;
    }

    /**
     * 增加No-op日志
     * @param term
     * @return
     */
    public NoOpEntry appendEntry(int term) {
        NoOpEntry noOpEntry = new NoOpEntry(entrySequence.getNextLogIndex(),term);
        entrySequence.append(noOpEntry);
        return noOpEntry;
    }

    /**
     * 追加普通日志
     * @param term
     * @param commandBytes
     * @return
     */
    public GeneralEntry appendEntry(int term, byte[] commandBytes) {

        GeneralEntry entry = new GeneralEntry(entrySequence.getNextLogIndex(),term,commandBytes);
        entrySequence.append(entry);
        return entry;
    }


    /**
     * 从leader增加日志，如果有不一致的日志，需要先移除在增加
     * @param prevLogIndex
     * @param prevLogTerm
     * @param leaderEntries
     * @return
     */
    public boolean appendEntriesFromLeader(int prevLogIndex, int prevLogTerm, List<Entry> leaderEntries) {

        if (!checkIfPreviousLogMatches(prevLogIndex,prevLogTerm)) {
            return false;
        }

        if (leaderEntries == null || leaderEntries.isEmpty()) {
            return true;
        }

        //移除冲突的日志条目
        EntrySequenceView newEntries = removeUnMatchedLog(new EntrySequenceView(leaderEntries));
        //仅追加日志
        appendEntriesFromLeader(newEntries);
        return true;
    }

    public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
        if (!validateNewCommitIndex(newCommitIndex,currentTerm)) {
            return;
        }

        logger.info("advance commit index from {} to {}",commitIndex, newCommitIndex);
        entrySequence.commit(newCommitIndex);

        //advanceApplyIndex();
    }



    private boolean validateNewCommitIndex(int newCommitIndex, int currentTerm) {
        if (newCommitIndex <= commitIndex) {
            return false;
        }
        Entry entry = entrySequence.getEntry(newCommitIndex);
        if (entry == null) {
            logger.debug("log of new commit index {} not found", newCommitIndex);
            return false;
        }
        if (entry.getTerm() != currentTerm) {
            logger.debug("log term of new commit index != current term ({} != {})", entry.getTerm(), currentTerm);
            return false;
        }
        return true;
    }

    private EntrySequenceView removeUnMatchedLog(EntrySequenceView view) {

        assert !view.isEmpty();
        int firstUnmatched = findFirtUnmatchedLog(view);
        if (firstUnmatched < 0) {
            return new EntrySequenceView(Collections.emptyList());
        }

        removeEntriesAfter(firstUnmatched - 1);
        return view.subView(firstUnmatched);
    }

    private int findFirtUnmatchedLog(EntrySequenceView view) {

        int logIndex;
        EntryMeta followerEntryMeta;
        for (Entry leadderEntry: view) {
            logIndex = leadderEntry.getIndex();
            followerEntryMeta = entrySequence.getEntryMeta(logIndex);
            if (followerEntryMeta == null || followerEntryMeta.getTerm() != leadderEntry.getTerm()) {
                return logIndex;
            }
        }
        return -1;
    }

    private void removeEntriesAfter(int index) {
        if (entrySequence.isEmpty() || index > entrySequence.getLastLogIndex()) {
            return;
        }

        logger.info("remove entries after {}", index);
        entrySequence.removeAfter(index);
    }

    private void appendEntriesFromLeader(EntrySequenceView view) {

        if (view.isEmpty()) {
            return;
        }

        logger.info("append entries fron {} to {}",view.getFirstLogIndex(),view.getLastLogIndex());

        for (Entry leaderEntry: view) {
            entrySequence.append(leaderEntry);
        }
    }


    //TODO
    private boolean checkIfPreviousLogMatches(int prevLogIndex, int prevLogTerm) {

        EntryMeta meta = entrySequence.getEntryMeta(prevLogIndex);

        if (meta == null) {
            logger.info("previous log {} not found", prevLogIndex);
            return false;
        }

        int term = meta.getTerm();
        if (term != prevLogTerm) {
            logger.warn("different term of previous log, local = {}, remote = {}",term,prevLogTerm);
            return false;
        }
        return true;
    }

    @Override
    public int getNextIndex() {
        return entrySequence.getNextLogIndex();
    }

    @Override
    public int getCommitIndex() {
        return commitIndex;
    }

    @Override
    public void close() {
//        snapshot.close();
        entrySequence.close();
//        snapshotBuilder.close();
//        stateMachine.shutdown();
    }


    private static class EntrySequenceView implements Iterable<Entry> {

        private final List<Entry> entries;

        private int firstLogIndex = -1;

        private int lastLogIndex = -1;

        EntrySequenceView(List<Entry> entries) {
            this.entries = entries;
            if (!entries.isEmpty()) {
                firstLogIndex = entries.get(0).getIndex();
                lastLogIndex = entries.get(entries.size() - 1).getIndex();
            }
        }

        /**
         * 获取指定位置的日志条目
         * @param index
         * @return
         */
        Entry getEntry(int index) {
            if (isEmpty() || index < firstLogIndex || index > lastLogIndex) {
                return null;
            }
            return entries.get(index - firstLogIndex);
        }

        boolean isEmpty() {
            return entries == null || entries.isEmpty();
        }

        int getFirstLogIndex() {
            return firstLogIndex;
        }

        int getLastLogIndex() {
            return lastLogIndex;
        }

        /**
         * 获取子视图
         * @param fromIndex
         * @return
         */
        EntrySequenceView subView(int fromIndex) {
            if (entries.isEmpty() || fromIndex > lastLogIndex) {
                return new EntrySequenceView(Collections.emptyList());
            }

            return new EntrySequenceView(entries.subList(fromIndex - firstLogIndex,entries.size()));
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }
    }
}
