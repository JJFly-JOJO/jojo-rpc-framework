package github.jojo.remoting.transport.netty.server;

import github.jojo.config.CustomShutdownHook;
import github.jojo.entity.RpcServiceProperties;
import github.jojo.factory.SingletonFactory;
import github.jojo.provider.ServiceProvider;
import github.jojo.provider.ServiceProviderImpl;
import github.jojo.remoting.transport.netty.codec.RpcMessageDecoder;
import github.jojo.remoting.transport.netty.codec.RpcMessageEncoder;
import github.jojo.utils.RuntimeUtil;
import github.jojo.utils.concurrent.threadpool.ThreadPoolFactoryUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 17:16
 * @description ---------------【服务端】接收客户端的message 调用message对应的方法 将结果返回给客户端-------------
 */
@Slf4j
@Component
public class NettyRpcServer {

    public static final int PORT = 9995;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);


    public void registerService(Object service, RpcServiceProperties rpcServiceProperties) {
        serviceProvider.publishService(service, rpcServiceProperties);
    }

    @SneakyThrows
    public void start() {
        //给当前服务器添加一个钩子 当服务器关闭时调用钩子处理逻辑
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        //获得IP
        String host = InetAddress.getLocalHost().getHostAddress();
        //采用一主多从的模式 boss只有一个线程处理accept连接事件 再分发给worker线程
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        //根据主机cpu核数创建自定义的线程池用于worker线程中 线程数量为cpu核数乘2
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtils.createThreadFactory("service-handler-group", false)
        );
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    //通过channel方法引导类ServerBootStrap指定IO模型为NIO
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立ESTABLISH频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化 为该channel分配专属执行链（ChannelPipeline）
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            //【自定义线程池】handler使用我们自定义的线程池 编码解码心跳包都走的IO线程
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            //绑定端口 同步等待绑定成功
            ChannelFuture f = b.bind(host, PORT).sync();
            //阻塞 等待服务端监听serverSocket端口关闭事件
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur exception when start server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }


}
