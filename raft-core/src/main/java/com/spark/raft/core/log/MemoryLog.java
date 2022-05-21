package com.spark.raft.core.log;

import com.spark.raft.core.log.sequence.EntrySequence;
import com.spark.raft.core.log.sequence.MemoryEntrySequence;

public class MemoryLog extends AbstractLog {

    public MemoryLog() {
        this(new MemoryEntrySequence());
    }

    public MemoryLog(EntrySequence sequence) {
        this.entrySequence = sequence;
    }
}
