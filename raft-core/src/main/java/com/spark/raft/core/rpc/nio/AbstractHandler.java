package com.spark.raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.Channel;
import com.spark.raft.core.rpc.message.*;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHandler extends ChannelDuplexHandler {

    private static Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

    protected final EventBus eventBus;

    NodeId remoteId;

    protected Channel channel;

    private AppendEntriesRpc lastAppendEntriesRpc;

    public AbstractHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        assert remoteId != null;
        assert channel != null;

        if (msg instanceof RequestVoteRpc) {
            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            eventBus.post(new RequestVoteRpcMessage(rpc,remoteId,channel));
        } else if (msg instanceof AppendEntriesResult) {
            AppendEntriesResult result = (AppendEntriesResult) msg;
            if (lastAppendEntriesRpc == null) {
                logger.warn("no last append entries rpc");
            } else {
                eventBus.post(new AppendEntriesResultMessage(result,remoteId,lastAppendEntriesRpc));
                lastAppendEntriesRpc = null;
            }
        }

    }

    @Override
    public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {

        if (msg instanceof AppendEntriesRpc) {
            lastAppendEntriesRpc = (AppendEntriesRpc) msg;
        }
        super.write(context,msg,promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn(cause.getMessage(), cause);
        ctx.close();
    }
}
