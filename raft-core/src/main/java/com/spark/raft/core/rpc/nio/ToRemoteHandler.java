package com.spark.raft.core.rpc.nio;

import com.google.common.eventbus.EventBus;
import com.spark.raft.core.node.NodeId;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToRemoteHandler extends AbstractHandler {

    private static final Logger logger = LoggerFactory.getLogger(ToRemoteHandler.class);

    private final NodeId selfId;

    public ToRemoteHandler(EventBus eventBus,NodeId remoteId, NodeId selfId) {
        super(eventBus);
        this.remoteId = remoteId;
        this.selfId = selfId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.write(selfId);
        channel = new NioChannel(ctx.channel());
    }
}
