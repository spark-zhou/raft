package com.spark.raft.core.node.store;

import com.google.common.io.Files;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.support.RandomAccessFileAdapter;
import com.spark.raft.core.support.SeekableFile;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public class FileNodeStore implements NodeStore {

    private static final String FILE_NAME = "node.bin";

    private static final long OFFSET_TERM = 0;

    private static long OFFSET_VOTED_FOR = 4;

    private final SeekableFile seekableFile;

    private int term;

    private NodeId votedFor;

    public FileNodeStore(File file) {
        try {
            if (!file.exists()) {
                Files.touch(file);
            }

            seekableFile = new RandomAccessFileAdapter(file);
            initializeOrLoad();
        }catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }

    public FileNodeStore(SeekableFile seekableFile) {
        this.seekableFile = seekableFile;
        try {
            initializeOrLoad();
        }catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }

    private void initializeOrLoad() throws IOException {
        if (seekableFile.size() == 0) {
            seekableFile.truncate(8L);
            seekableFile.seek(0);
            seekableFile.writeInt(0);  //term
            seekableFile.writeInt(0);  //votedFor length
        } else {

            //读取term
            term = seekableFile.readInt();

            //读取votedFor
            int length = seekableFile.readInt();
            if (length > 0) {
                byte [] bytes = new byte[length];
                seekableFile.read(bytes);
                votedFor = new NodeId(new String(bytes));
            }
        }
    }


    @Override
    public int getTerm() {
        return this.term;
    }


    @Override
    public void setTerm(int term) {
        try {
            seekableFile.seek(OFFSET_TERM);
            seekableFile.writeInt(term);
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
        this.term = term;
    }


    @Nullable
    @Override
    public NodeId getVotedFor() {
        return this.votedFor;
    }


    @Override
    public void setVotedFor(@Nullable NodeId votedFor) {

        try {
            seekableFile.seek(OFFSET_VOTED_FOR);
            if (votedFor == null) {
                seekableFile.writeInt(0);
                seekableFile.truncate(8L);
            } else {
                byte[] bytes = votedFor.getValue().getBytes();
                seekableFile.writeInt(bytes.length);
                seekableFile.write(bytes);
            }
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
        this.votedFor = votedFor;
    }


    @Override
    public void close() {
        try {
            seekableFile.close();
        } catch (IOException e) {
            throw new NodeStoreException(e);
        }
    }
}
