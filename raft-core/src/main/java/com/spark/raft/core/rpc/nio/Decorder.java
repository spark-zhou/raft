package com.spark.raft.core.rpc.nio;

import com.spark.raft.core.Protos;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.message.RequestVoteRpc;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import static com.spark.raft.core.rpc.message.MessageConstants.MSG_TYPE_NODE_ID;
import static com.spark.raft.core.rpc.message.MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC;

public class Decorder extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        int availableBytes = in.readableBytes();
        if (availableBytes < 8) {
            return;
        }

        in.markReaderIndex();
        int messageType = in.readInt();
        int payloadLength = in.readInt();

        if (in.readableBytes() < payloadLength) {
            in.resetReaderIndex();
            return;
        }

        byte [] payload = new byte[payloadLength];
        in.readBytes(payload);

        switch (messageType) {
            case MSG_TYPE_NODE_ID:
                out.add(new NodeId(new String(payload)));
                break;
            case MSG_TYPE_REQUEST_VOTE_RPC:
                Protos.RequestVoteRpc protoRpc = Protos.RequestVoteRpc.parseFrom(payload);

                RequestVoteRpc rpc = new RequestVoteRpc();
                rpc.setTerm(protoRpc.getTerm());
                rpc.setCandidateId(new NodeId(protoRpc.getCandidateId()));
                rpc.setLastLogIndex(protoRpc.getLastLogIndex());
                rpc.setLastLogTerm(protoRpc.getLasLogTerm());
                out.add(rpc);
                break;
        }
    }
}
