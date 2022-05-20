package com.spark.raft.core.log.entry;

public class EntryFactory {

    public static Entry create(int kind, int index, int term, byte[] commandBytes) {

        switch (kind) {
            case Entry.KIND_NO_OP:
                return new NoOpEntry(index,term);
            case Entry.KIND_GENERAL:
                return new GeneralEntry(index,term,commandBytes);
            default:
                throw new IllegalArgumentException("unexpected entry kind " + kind);
        }
    }
}
