package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Date;

/**
 * 模拟词频统计的服务端
 */
public class CounterResovlerPacketServer {
    public void bind(int port)throws Exception{
        // 网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup WorkerGroup = new NioEventLoopGroup();

        try {
            // ServerBootstrap 类，是启动NIO服务器的辅助启动类
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup,WorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childHandler(new ChildChannelHandler());

            // 绑定端口,同步等待成功
            ChannelFuture f= b.bind(port).sync();

            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        }finally {
            // 释放线程池资源
            bossGroup.shutdownGracefully();
            WorkerGroup.shutdownGracefully();
        }
    }

    /**
     * 初始化channel的handler
     */
    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected  void initChannel(SocketChannel ch)throws Exception{
            //增加换行符解码器
            ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
            ch.pipeline().addLast(new StringDecoder());
            ch.pipeline().addLast(new CounterServerHandler());
        }
    }

    /**
     * 法务端业务处理的handler
     */
    public class CounterServerHandler extends ChannelInboundHandlerAdapter {

        private int counter;

        // 用于网络的读写操作

        public void channelRead(ChannelHandlerContext ctx, Object msg)
                throws Exception{
            String body = (String)msg;
            System.out.println("服务端接受到的数据 : " + body+";累加频率是:"+ (++counter));
            String currentTime = "烧烤小分队带你走上IT巅峰".equalsIgnoreCase(body)?new Date(System.currentTimeMillis()).toString():"错误数据";
            currentTime +=System.getProperty("line.separator");
            ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
            ctx.writeAndFlush(resp);

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
            ctx.close();
        }
    }


    public static void main(String[]args)throws Exception{
        int port = 8080;
        if(args!=null && args.length>0){
            try {
                port = Integer.valueOf(args[0]);
            }
            catch (NumberFormatException ex){}
        }
        new CounterResovlerPacketServer().bind(port);
    }
}
