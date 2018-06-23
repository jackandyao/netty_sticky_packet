package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * Create jhp
 */
public class SplitPacketServer {
    private final int port;//定义服务器端监听的端口
    /** 构造函数中传入参数 **/
    public SplitPacketServer(int port){
        this.port = port;
    }

    /** 启动服务器 **/
    public void start() throws Exception{
        //县城组
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();

        //创建一个serverbootstrap实例
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        try {
            serverBootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)//指定使用一个NIO传输Channel
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        //在channel的ChannelPipeline中加入EchoServerHandler到最后
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new FixedLengthFrameDecoder(36));

                            channel.pipeline().addLast(new StringDecoder());

                            channel.pipeline().addLast(new EchoServerHandler());
                        }
                    });
            //异步的绑定服务器,sync()一直等到绑定完成.
            ChannelFuture future = serverBootstrap.bind(this.port).sync();
            System.out.println(SplitPacketServer.class.getName()+" started and listen on '"+ future.channel().localAddress());
            future.channel().closeFuture().sync();//获得这个channel的CloseFuture,阻塞当前线程直到关闭操作完成
        } finally {
            boss.shutdownGracefully().sync();//关闭group,释放所有的资源
            worker.shutdownGracefully().sync();//关闭group,释放所有的资源
        }
    }


    /**
     * main
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new SplitPacketServer(8000).start();
    }

    class EchoServerHandler extends ChannelInboundHandlerAdapter {

        private int counter=0;
        /**
         * 每次收到消息的时候被调用;
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String body = (String)msg;

            System.out.println("this is:"+ (++counter) +" time." + " Server received: " + body);

            ByteBuf echo = Unpooled.copiedBuffer(body.getBytes());

            ctx.writeAndFlush(echo);
        }


        /**
         * 在读操作异常被抛出时被调用
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();//打印异常的堆栈跟踪信息
            ctx.close();//关闭这个channel
        }
    }


}