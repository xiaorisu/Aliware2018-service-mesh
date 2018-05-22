package com.alibaba.dubbo.performance.demo.agent.consumeragent.tcp;

import com.alibaba.dubbo.performance.demo.agent.consumeragent.model.ChannelHolder;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.utils.EnumKey;
import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class TcpConsumerMsgHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static Log log = LogFactory.getLog(TcpConsumerMsgHandler.class);

    private static int genId=0;

    private static Random random = new Random();

//    private static Map<EnumKey,TcpChannel> tcpChannelMap;

    private static Channel channelSmall;

    private static Channel channelMedium;

    private static Channel channelLarge;

    public TcpConsumerMsgHandler (Map<EnumKey,TcpChannel> tcpChannelMap){
//        this.tcpChannelMap=tcpChannelMap;
        try {
            channelSmall=tcpChannelMap.get(EnumKey.S).getChannel();
            channelMedium=tcpChannelMap.get(EnumKey.M).getChannel();
            channelLarge=tcpChannelMap.get(EnumKey.L).getChannel();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception{

        ByteBuf buf = msg.content();
//        System.out.println(buf.toString(io.netty.util.CharsetUtil.UTF_8));


        ChannelHolder.put(genId,ctx.channel());

//        PooledByteBufAllocator.DEFAULT.
        CompositeByteBuf sendBuf=ctx.alloc().compositeDirectBuffer();
        ByteBuf idBuf=ctx.alloc().ioBuffer();
        idBuf.writeInt(genId);
        sendBuf.addComponents(true,idBuf,buf.slice(136,buf.readableBytes()-136).retain());
        //sendBuf.writeBytes(System.lineSeparator().getBytes());

        Channel ch=null;

        //tcp按照性能简单负载均衡,fix me:利用id 可以不生成随机数
        int x=random.nextInt(6);
        if(x==0){
            ch=channelSmall;
        }else if(x<=2){
            ch=channelMedium;
        }else{
            ch=channelLarge;
        }
//        ch=tcpChannelMap.get(EnumKey.getNext(id));

        //idea下测试使用tcp
      //  ch=tcpChannelMap.get("ideaTest");

        ++genId;
        /*tcp发给provider agent*/
//        System.out.println("send start..");
        ch.writeAndFlush(sendBuf);
//        System.out.println("send finish..");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //ctx.close();
    }

}
