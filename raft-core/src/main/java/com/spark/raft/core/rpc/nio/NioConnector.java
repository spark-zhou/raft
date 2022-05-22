package com.spark.raft.core.rpc.nio;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.spark.raft.core.node.NodeEndpoint;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.Channel;
import com.spark.raft.core.rpc.ChannelConnectException;
import com.spark.raft.core.rpc.Connector;
import com.spark.raft.core.rpc.message.AppendEntriesResult;
import com.spark.raft.core.rpc.message.AppendEntriesRpc;
import com.spark.raft.core.rpc.message.RequestVoteResult;
import com.spark.raft.core.rpc.message.RequestVoteRpc;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NioConnector implements Connector {

    private static Logger logger = LoggerFactory.getLogger(NioConnector.class);

    private final NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private final NioEventLoopGroup workGroup;

    private final boolean workGroupShared;

    private final EventBus eventBus;

    private final int port;

    private final InboundChannelGroup inboundChannelGroup = new InboundChannelGroup();

    private final OutboundChannelGroup outboundChannelGroup;

    private final ExecutorService executorService = Executors.newCachedThreadPool((r) -> {
        Thread thread = new Thread(r);
        thread.setUncaughtExceptionHandler((t, e) -> {
            logException(e);
        });
        return thread;
    });

    public NioConnector(NioEventLoopGroup workGroup, boolean workGroupShared, NodeId selfId, EventBus eventBus, int port,int logReplicationInterval) {
        this.workGroup = workGroup;
        this.workGroupShared = workGroupShared;
        this.eventBus = eventBus;
        this.port = port;
        this.outboundChannelGroup = new OutboundChannelGroup(workGroup,eventBus,selfId,logReplicationInterval);
    }

    @Override
    public void initialize() {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup,workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new Decorder());
                        pipeline.addLast(new Encorder());
                        pipeline.addLast(new FromRemoteHandler(eventBus,inboundChannelGroup));
                    }
                });

        logger.info("node listen on port {}",port);
        try {
            bootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to bind port",e);
        }
    }

    @Override
    public void sendRequestVote(RequestVoteRpc rpc, @Nonnull  Collection<NodeEndpoint> destinationEndPoints) {

        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndPoints);
        for (NodeEndpoint endpoint : destinationEndPoints) {
            logger.debug("send {} to node {}", rpc, endpoint.getId());
            executorService.execute(() -> getChannel(endpoint).writeRequestVoteRpc(rpc));
        }
    }

    @Override
    public void replyRequestVote(RequestVoteResult result, @Nonnull NodeEndpoint destinationEndPoint) {
        Preconditions.checkNotNull(result);
        logger.debug("reply {} to node {}", result, destinationEndPoint.getId());
        try {
            executorService.execute(() -> getChannel(destinationEndPoint).writeRequestVoteResult(result));
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void sendAppendEnrties(AppendEntriesRpc rpc, NodeEndpoint destinationEndPoint) {
        Preconditions.checkNotNull(rpc);
        Preconditions.checkNotNull(destinationEndPoint);
        logger.debug("send {} to node {}", rpc, destinationEndPoint.getId());
        executorService.execute(() -> getChannel(destinationEndPoint).writeAppendEntriesRpc(rpc));
    }

    @Override
    public void replyAppendEnrties(AppendEntriesResult result, NodeEndpoint destinationEndPoint) {

        Preconditions.checkNotNull(result);
        logger.debug("reply {} to node {}", result, destinationEndPoint.getId());
        try {
            executorService.execute(() -> getChannel(destinationEndPoint).writeAppendEntriesResult(result));
        } catch (Exception e) {
            logException(e);
        }
    }

    @Override
    public void resetChannels() {
        inboundChannelGroup.closeAll();
    }

    @Override
    public void close() {

        logger.info("close connector...");
        inboundChannelGroup.closeAll();
        outboundChannelGroup.closeAll();
        bossGroup.shutdownGracefully();

        if (!workGroupShared) {
            workGroup.shutdownGracefully();
        }
    }


    private Channel getChannel(NodeEndpoint endpoint) {
        return outboundChannelGroup.getOrConnect(endpoint.getId(), endpoint.getAddress());
    }

    private void logException(Throwable e) {
        if (e instanceof ChannelConnectException) {
            logger.warn(e.getMessage());
        } else {
            logger.warn("failed to process channel", e);
        }
    }
}
