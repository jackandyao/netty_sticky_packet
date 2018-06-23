package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;

/**
 * Created by louyuting on 16/11/27.
 */
public class SplitPacketClient {
    private final String host;
    private final int port;//定义服务器端监听的端口
    /** 构造函数中传入参数 **/
    public SplitPacketClient(String host, int port){
        this.host = host;
        this.port = port;
    }

    /** 启动服务器 **/
    public void start() throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        //创建一个client 的bootstrap实例
        Bootstrap clientBootstrap = new Bootstrap();

        try {
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)//指定使用一个NIO传输Channel
                    .remoteAddress(new InetSocketAddress(host, port))//设置远端服务器的host和端口
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        //在channel的ChannelPipeline中加入EchoClientHandler到最后
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(new FixedLengthFrameDecoder(36));


                            channel.pipeline().addLast(new StringDecoder());
                            channel.pipeline().addLast(new EchoClientHandler());
                        }
                    });
            ChannelFuture f = clientBootstrap.connect().sync();//连接到远端,一直等到连接完成
            f.channel().closeFuture().sync();//一直阻塞到channel关闭
        } finally {
            group.shutdownGracefully().sync();//关闭group,释放所有的资源
        }
    }


    /**
     * main
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new SplitPacketClient("127.0.0.1", 8000).start();
    }


    class EchoClientHandler extends SimpleChannelInboundHandler<String> {

        private int counter=0;

        private static final String REQ = "this is hello word please come to me";

        /**
         * 当收到连接成功的通知,发送一条消息.
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            for(int i=0; i<10; i++){
                ctx.writeAndFlush( Unpooled.copiedBuffer(REQ.getBytes()) );
            }
        }

        /**
         * 每当收到数据时这个方法会被调用.打印收到的消息日志
         * @param channelHandlerContext
         * @param msg
         * @throws Exception
         */
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
            System.out.println("client received: " + "counter:" + (++counter) + "  msg:"+msg);
        }

        /**
         * 异常发生时,记录错误日志,关闭channel
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();//打印堆栈的错误日志
            ctx.close();
        }

    }

}