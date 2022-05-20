package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.EntryFactory;
import com.spark.raft.core.support.RandomAccessFileAdapter;
import com.spark.raft.core.support.SeekableFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 日志条目文件
 */
public class EntriesFile {

    private final SeekableFile seekableFile;

    public EntriesFile(File file) throws FileNotFoundException {
        this(new RandomAccessFileAdapter(file));
    }

    public EntriesFile(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
    }

    /**
     * 追加日志条目,日志条目数据格式: kind  index  term commandBytesLength  commandBytes
     * @param entry
     * @return
     */
    public long appendEntry(Entry entry) throws IOException {

        long offset = seekableFile.size();
        seekableFile.seek(offset);
        seekableFile.writeInt(entry.getKind());
        seekableFile.writeInt(entry.getIndex());
        seekableFile.writeInt(entry.getTerm());

        byte [] commandBytes = entry.getCommandBytes();
        seekableFile.writeInt(commandBytes.length);
        seekableFile.write(commandBytes);
        return offset;
    }

    /**
     * 从偏移量加载日志条目
     * @param offset
     * @return
     */
    public Entry loadEntry(long offset) throws IOException {

        if (offset > seekableFile.size()) {
            throw new IllegalArgumentException("offset must be less than file size.");
        }

        seekableFile.seek(offset);
        int kind = seekableFile.readInt();
        int index = seekableFile.readInt();
        int term = seekableFile.readInt();
        int length = seekableFile.readInt();
        byte [] commandBytes = new byte[length];
        seekableFile.read(commandBytes);
        return EntryFactory.create(kind,index,term,commandBytes);
    }

    public long size() throws IOException {
        return seekableFile.size();
    }

    public void truncate(long offset) throws IOException {
        seekableFile.truncate(offset);
    }

    public void clear() throws IOException {
        truncate(0L);
    }

    public void close() throws IOException {
        seekableFile.close();
    }
}
