package com.spark.raft.core.log;

import com.spark.raft.core.log.sequence.FileEntrySequence;

import java.io.File;

public class FileLog extends AbstractLog {

    private final RootDir rootDir;

    public FileLog(File baseDir) {

        rootDir = new RootDir(baseDir);

        LogGeneration latestGeneration = rootDir.getLatestGeneration();
        if (latestGeneration != null) {
            entrySequence = new FileEntrySequence(latestGeneration,latestGeneration.getLastIncludedIndex());
        } else {
            LogGeneration firtGeneration  = rootDir.createFirstGeneration();
            entrySequence = new FileEntrySequence(firtGeneration,1);
        }
    }
 }
