package com.spark.raft.core.rpc;

public class ChannelException extends RuntimeException{

    public ChannelException(Throwable cause) {
        super(cause);
    }

    public ChannelException(String message, Throwable cause) {
        super(message, cause);
    }
}
