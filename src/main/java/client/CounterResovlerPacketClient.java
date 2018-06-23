package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * 模拟发送数据的客户端
 */
public class CounterResovlerPacketClient {

    public void connect(String host,int port)throws Exception{
        // 配置服务端的NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // Bootstrap 类，是启动NIO服务器的辅助启动类
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception{
                            //增加换行解码器
                            ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new CounterClientHandler());
                        }
                    });

            // 发起异步连接操作
            ChannelFuture f= b.connect(host,port).sync();

            // 等待客服端链路关闭
            f.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
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
        new CounterResovlerPacketClient().connect("127.0.0.1",port);
    }

    /**
     * 客户端发送数据的handler
     */
    class CounterClientHandler extends ChannelInboundHandlerAdapter {

        //统计频率
        private int counter;

        public void channelRead(ChannelHandlerContext ctx, Object msg)
                throws Exception{
            String body = (String)msg;
            System.out.println("客户端接受服务度返回的数据,当前时间是 : " + body+";当前频率是 : "+ ++counter);
        }

        /**
         * 客户端连接上服务端之后会调用该方法
         * @param ctx
         */
        public void channelActive(ChannelHandlerContext ctx){
            ByteBuf firstMessage=null;
            String value = "烧烤小分队带你走上IT巅峰";
            String data = value + "\n";
            for (int i=0;i<10;i++){
                firstMessage = Unpooled.buffer(data.getBytes().length);
                firstMessage.writeBytes(data.getBytes());
                ctx.writeAndFlush(firstMessage);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
            System.out.println(("message from:"+cause.getMessage()));
            ctx.close();
        }
    }

}


