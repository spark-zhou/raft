package com.spark.raft.core.rpc.nio;

import com.google.protobuf.MessageLite;
import com.spark.raft.core.Protos;
import com.spark.raft.core.node.NodeId;
import com.spark.raft.core.rpc.message.MessageConstants;
import com.spark.raft.core.rpc.message.RequestVoteRpc;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Encorder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf byteBuf) throws Exception {

        if (msg instanceof NodeId) {
            this.writeMessage(byteBuf, MessageConstants.MSG_TYPE_NODE_ID,((NodeId)msg).getValue().getBytes());
        } else if (msg instanceof RequestVoteRpc) {

            RequestVoteRpc rpc = (RequestVoteRpc) msg;
            Protos.RequestVoteRpc protocRpc = Protos.RequestVoteRpc.newBuilder()
                    .setTerm(rpc.getTerm())
                    .setCandidateId(rpc.getCandidateId().getValue())
                    .setLastLogIndex(rpc.getLastLogIndex())
                    .setLasLogTerm(rpc.getLastLogTerm())
                    .build();
            this.writeMessage(byteBuf,MessageConstants.MSG_TYPE_REQUEST_VOTE_RPC,protocRpc);
        }
    }

    private void writeMessage(ByteBuf out, int messageType, MessageLite message) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        message.writeTo(byteOutput);
        out.writeInt(messageType);
        this.writeBytes(out, byteOutput.toByteArray());
    }

    private void writeMessage(ByteBuf out, int messageType, byte[] bytes) {
        // 4 + 4 + VAR
        out.writeInt(messageType);
        this.writeBytes(out, bytes);
    }

    private void writeBytes(ByteBuf out, byte[] bytes) {
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
