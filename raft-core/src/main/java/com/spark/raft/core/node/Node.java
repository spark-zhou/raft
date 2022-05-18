package com.spark.raft.core.node;

public interface Node {

    //启动
    void start();

    //关闭
    void stop() throws InterruptedException;
}
