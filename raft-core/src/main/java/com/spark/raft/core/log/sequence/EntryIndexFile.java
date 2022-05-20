package com.spark.raft.core.log.sequence;

import com.spark.raft.core.support.RandomAccessFileAdapter;
import com.spark.raft.core.support.SeekableFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EntryIndexFile implements Iterable<EntryIndexItem>{

    /**
     * 最大条目索引的偏移
     */
    private static final long OFFSET_MAX_ENTRY_INDEX = Integer.BYTES;

    /**
     * 单条日志条目元信息的长度
     */
    private static final int LENGTH_ENTRY_INDEX_ITEM = 16;

    private final SeekableFile seekableFile;

    /**
     * 日志条目数
     */
    private int entryIndexCount;

    /**
     * 最小日志索引
     */
    private int minEntryIndex;

    /**
     * 最大日志索引
     */
    private int maxEntryIndex;

    private Map<Integer,EntryIndexItem> entryIndexMap = new HashMap<>();

    public EntryIndexFile(File file) throws IOException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntryIndexFile(SeekableFile seekableFile) throws IOException {
        this.seekableFile = seekableFile;
        load();
    }

    private void load() throws IOException {

        if (seekableFile.size() == 0L) {
            entryIndexCount = 0;
            return;
        }

        minEntryIndex = seekableFile.readInt();
        maxEntryIndex = seekableFile.readInt();
        entryIndexCount = maxEntryIndex - minEntryIndex + 1;

        long offset;
        int kind,term;
        for (int i = minEntryIndex; i <= maxEntryIndex; i++) {
            offset = seekableFile.readLong();
            kind = seekableFile.readInt();
            term = seekableFile.readInt();
            entryIndexMap.put(i,new EntryIndexItem(i,offset,kind,term));
        }
    }

    public boolean isEmpty() {
        return entryIndexCount == 0;
    }

    /**
     * 追加日志条目索引信息
     * @param index
     * @param offset
     * @param kind
     * @param term
     */
    public void appendEntryIndex(int index, long offset, int kind, int term) throws IOException {

        if (seekableFile.size() == 0L) {
            seekableFile.writeInt(index);
            minEntryIndex = index;
        } else {
            if (index != maxEntryIndex + 1) {
                throw new IllegalArgumentException("index must be " + (maxEntryIndex + 1) + ", but was " + index);
            }
            seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        }
        //写入maxEntryIndex
        seekableFile.writeInt(index);
        maxEntryIndex = index;

        entryIndexCount = maxEntryIndex - minEntryIndex + 1;

        seekableFile.seek(getOffsetEntryIndexItem(index));
        seekableFile.writeLong(offset);
        seekableFile.writeInt(kind);
        seekableFile.writeInt(term);
        entryIndexMap.put(index,new EntryIndexItem(index,offset,kind,term));
    }


    /**
     * 清空
     * @throws IOException
     */
    public void clear() throws IOException {

        seekableFile.truncate(0L);
        entryIndexCount = 0;
        entryIndexMap.clear();
    }

    /**
     * 移出某个索引之后的数据
     * @param newMaxEntryIndex
     */
    public void removeAfter(int newMaxEntryIndex) throws IOException {

        if (isEmpty() || newMaxEntryIndex >= maxEntryIndex) {
            return;
        }

        if(newMaxEntryIndex < minEntryIndex) {
            clear();
            return;
        }

        seekableFile.seek(OFFSET_MAX_ENTRY_INDEX);
        seekableFile.writeInt(newMaxEntryIndex);
        seekableFile.truncate(getOffsetEntryIndexItem(newMaxEntryIndex + 1));

        for (int i = newMaxEntryIndex + 1; i <= maxEntryIndex; i++) {
            entryIndexMap.remove(i);
        }

        maxEntryIndex = newMaxEntryIndex;
        entryIndexCount = newMaxEntryIndex - minEntryIndex + 1;
    }

    /**
     * 获取指定索引的日志的偏移
     * @param index
     * @return
     */
    private long getOffsetEntryIndexItem(int index) {
        return (index - minEntryIndex) * LENGTH_ENTRY_INDEX_ITEM + Integer.BYTES * 2;
    }

    @Override
    public Iterator<EntryIndexItem> iterator() {

        if (isEmpty()) {
            return Collections.emptyIterator();
        }

        return new EntryIndexIterator(entryIndexCount,minEntryIndex);
    }

    public long getOffset(int entryIndex) {
        return get(entryIndex).getOffset();
    }

    @Nonnull
    public EntryIndexItem get(int entryIndex) {
        checkEmpty();
        if (entryIndex < minEntryIndex || entryIndex > maxEntryIndex) {
            throw new IllegalArgumentException("index < min or index > max");
        }
        return entryIndexMap.get(entryIndex);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("no entry index");
        }
    }

    public int getEntryIndexCount() {
        return entryIndexCount;
    }

    public int getMinEntryIndex() {
        return minEntryIndex;
    }

    public int getMaxEntryIndex() {
        return maxEntryIndex;
    }

    public Map<Integer, EntryIndexItem> getEntryIndexMap() {
        return entryIndexMap;
    }


    public void close() throws IOException {
        seekableFile.close();
    }

    private class EntryIndexIterator implements Iterator<EntryIndexItem> {

        private final int entryIndexCount;

        private int currentEntryIndex;

        public EntryIndexIterator(int entryIndexCount, int minEntryIndex) {
            this.entryIndexCount = entryIndexCount;
            this.currentEntryIndex = minEntryIndex;
        }

        @Override
        public boolean hasNext() {
            checkModification();
            return currentEntryIndex <= maxEntryIndex;
        }

        private void checkModification() {
            if (this.entryIndexCount != EntryIndexFile.this.entryIndexCount) {
                throw new IllegalStateException("entry index count changed.");
            }
        }

        @Override
        public EntryIndexItem next() {
            checkModification();
            return entryIndexMap.get(currentEntryIndex++);
        }
    }
}
