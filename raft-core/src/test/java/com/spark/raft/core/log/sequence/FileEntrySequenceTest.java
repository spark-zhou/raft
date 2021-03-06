package com.spark.raft.core.log.sequence;

import com.spark.raft.core.log.entry.Entry;
import com.spark.raft.core.log.entry.NoOpEntry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileEntrySequenceTest {

    private EntriesFile entriesFile;

    private EntryIndexFile entryIndexFile;


    @Before
    public void setUp() throws IOException {
        File file = new File("/Users/spark.zhou/raft-test/entry_file");
        entriesFile = new EntriesFile(file);
        File idxFile = new File("/Users/spark.zhou/raft-test/entry_index_file");
        entryIndexFile = new EntryIndexFile(idxFile);
    }

    @After
    public void clear() throws IOException {
        if (entriesFile != null) {
            entriesFile.clear();
            entriesFile.close();
        }

        if (entryIndexFile != null) {
            entryIndexFile.clear();
            entryIndexFile.close();
        }
    }

    @Test
    public void testInitialize() throws IOException {
//        entriesFile.appendEntry(new NoOpEntry(1,1));
        entryIndexFile.appendEntryIndex(1,0L,1,1);
        entryIndexFile.appendEntryIndex(2,20L,1,1);
        FileEntrySequence sequence = new FileEntrySequence(entriesFile,entryIndexFile,1);
        Assert.assertEquals(3,sequence.getNextLogIndex());
        Assert.assertEquals(1,sequence.getFirstLogIndex());
        Assert.assertEquals(2,sequence.getLastLogIndex());
        Assert.assertEquals(0, sequence.getCommitIndex());
    }

    @Test
    public void testAppendEntry() {
        FileEntrySequence sequence = new FileEntrySequence(entriesFile,entryIndexFile,1);
        Assert.assertEquals(1,sequence.getNextLogIndex());
        sequence.append(new NoOpEntry(1,1));
        Assert.assertEquals(2,sequence.getNextLogIndex());
        Assert.assertEquals(1,sequence.getLastEntry().getIndex());
    }

    @Test
    public void testGetEntry() throws IOException {
        Entry entry = new NoOpEntry(1,1);
        long offset = entriesFile.appendEntry(entry);
        entryIndexFile.appendEntryIndex(entry.getIndex(),offset,entry.getKind(),entry.getTerm());

        FileEntrySequence sequence = new FileEntrySequence(entriesFile,entryIndexFile,1);
        sequence.append(new NoOpEntry(2,1));

        Assert.assertNull(sequence.getEntry(0));
        Assert.assertEquals(1, sequence.getEntry(1).getIndex());
        Assert.assertEquals(2,sequence.getEntry(2).getIndex());
        Assert.assertNull(sequence.getEntry(3));
    }

    @Test
    public void testSublist() throws IOException {
        appendEntryToFile(new NoOpEntry(1,1));
        appendEntryToFile(new NoOpEntry(2,2));

        FileEntrySequence sequence = new FileEntrySequence(entriesFile,entryIndexFile,1);
        sequence.append(new NoOpEntry(sequence.getNextLogIndex(),3));
        sequence.append(new NoOpEntry(sequence.getNextLogIndex(),4));

        List<Entry> subList = sequence.subList(2);

        Assert.assertEquals(3,subList.size());
        Assert.assertEquals(2,subList.get(0).getIndex());
        Assert.assertEquals(4,subList.get(2).getIndex());
    }

    @Test
    public void testRemoveAfterEntriesInFile() throws IOException {
        appendEntryToFile(new NoOpEntry(1,1));
        appendEntryToFile(new NoOpEntry(2,1));

        FileEntrySequence sequence = new FileEntrySequence(entriesFile,entryIndexFile,1);

        sequence.append(new NoOpEntry(3,2));
        Assert.assertEquals(1,sequence.getFirstLogIndex());
        Assert.assertEquals(3,sequence.getLastLogIndex());

        sequence.removeAfter(1);
        Assert.assertEquals(1,sequence.getFirstLogIndex());
        Assert.assertEquals(1,sequence.getLastLogIndex());
    }


    private void appendEntryToFile(Entry entry) throws IOException {
        long offset = entriesFile.appendEntry(entry);
        entryIndexFile.appendEntryIndex(entry.getIndex(),offset,entry.getKind(),entry.getTerm());
    }
}
