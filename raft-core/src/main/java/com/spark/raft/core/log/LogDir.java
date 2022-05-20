package com.spark.raft.core.log;

import java.io.File;

public interface LogDir {

    /**
     * 初始化目录
     */
    void initialize();

    /**
     * 目录是否存在
     * @return
     */
    boolean exists();

    /**
     * 获取EntriesFile对应的文件
     * @return
     */
    File getEntriesFile();

    /**
     * 获取EntriesIndexFile对应的文件
     * @return
     */
    File getEntryOffsetIndexFile();

    /**
     * 获取目录
     * @return
     */
    File get();

    /**
     * 重命名目录
     * @param logDir
     * @return
     */
    boolean renameTo(LogDir logDir);
}
