package com.alibaba.dubbo.performance.demo.agent.provideragent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

public class TCPConsumerAgentMsgHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        long id=byteBuf.readLong();
       // System.out.println(id);
//        byte[] bytes = new byte[buf.readableBytes()];
//        buf.readBytes(bytes);
        ByteBuf dataBuf=byteBuf.slice(8,byteBuf.readableBytes());
       // System.out.println(dataBuf.toString(CharsetUtil.UTF_8));
        byte [] data=java.lang.String.valueOf(dataBuf.toString(io.netty.util.CharsetUtil.UTF_8).hashCode()).getBytes();
        ByteBuf sendBuf=ctx.alloc().ioBuffer();
        sendBuf.writeLong(id);
        sendBuf.writeBytes(data);
        sendBuf.writeBytes(System.lineSeparator().getBytes());
       // System.out.println(sendBuf.toString(CharsetUtil.UTF_8));
        ctx.writeAndFlush(sendBuf);
    }
}
