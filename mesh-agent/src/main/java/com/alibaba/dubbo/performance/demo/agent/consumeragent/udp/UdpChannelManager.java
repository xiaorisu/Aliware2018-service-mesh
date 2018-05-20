package com.alibaba.dubbo.performance.demo.agent.consumeragent.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;


public class UdpChannelManager {

    private static Channel channel=null;

    public UdpChannelManager(){

    }

    public static void initChannel(EventLoopGroup group) throws Exception{

        Class<? extends DatagramChannel> channelClass= Epoll.isAvailable() ? EpollDatagramChannel.class:NioDatagramChannel.class;

        channel = new Bootstrap()
                .group(group)
                //.channel(EpollDatagramChannel.class)
                .channel(channelClass)
                //.option(ChannelOption.SO_BACKLOG, 128)    //设置缓存队列
                //.option(ChannelOption.SO_RCVBUF, 1024 * 1024)// 设置UDP读缓冲区为1M
                //.option(ChannelOption.SO_SNDBUF, 1024 * 1024)// 设置UDP写缓冲区为1M ;
                .option(EpollChannelOption.SO_REUSEPORT,true)
                .handler(new UdpProviderAgentMsgHandler())
                .bind(new InetSocketAddress(20000)).sync().channel();
    }

    public static Channel getChannel() throws Exception {
        return channel;
    }

}