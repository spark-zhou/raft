package com.spark.raft.core.rpc.nio;

import com.spark.raft.core.rpc.Channel;
import com.spark.raft.core.rpc.ChannelException;
import com.spark.raft.core.rpc.message.AppendEntriesResult;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;
import com.spark.raft.core.rpc.message.RequestVoteResult;
import com.spark.raft.core.rpc.message.RequestVoteRpc;

import javax.annotation.Nonnull;

public class NioChannel implements Channel {

    private final io.netty.channel.Channel nettyChannel;

    NioChannel(io.netty.channel.Channel nettyChannel) {
        this.nettyChannel = nettyChannel;
    }

    @Override
    public void writeRequestVoteRpc(@Nonnull RequestVoteRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeRequestVoteResult(@Nonnull RequestVoteResult result) {
        nettyChannel.writeAndFlush(result);
    }

    @Override
    public void writeAppendEntriesRpc(@Nonnull AppendEntriesRpc rpc) {
        nettyChannel.writeAndFlush(rpc);
    }

    @Override
    public void writeAppendEntriesResult(@Nonnull AppendEntriesResult result) {
        nettyChannel.writeAndFlush(result);
    }

//    @Override
//    public void writeInstallSnapshotRpc(@Nonnull InstallSnapshotRpc rpc) {
//        nettyChannel.writeAndFlush(rpc);
//    }
//
//    @Override
//    public void writeInstallSnapshotResult(@Nonnull InstallSnapshotResult result) {
//        nettyChannel.writeAndFlush(result);
//    }

    @Override
    public void close() {
        try {
            nettyChannel.close().sync();
        } catch (InterruptedException e) {
            throw new ChannelException("failed to close", e);
        }
    }

    io.netty.channel.Channel getDelegate() {
        return nettyChannel;
    }
}
