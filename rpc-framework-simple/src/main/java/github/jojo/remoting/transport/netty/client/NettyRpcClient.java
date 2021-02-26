package github.jojo.remoting.transport.netty.client;

import github.jojo.enums.CompressTypeEnum;
import github.jojo.enums.SerializationTypeEnum;
import github.jojo.extension.ExtensionLoader;
import github.jojo.factory.SingletonFactory;
import github.jojo.registry.ServiceDiscovery;
import github.jojo.remoting.constants.RpcConstants;
import github.jojo.remoting.dto.RpcMessage;
import github.jojo.remoting.dto.RpcRequest;
import github.jojo.remoting.dto.RpcResponse;
import github.jojo.remoting.transport.RpcRequestTransport;
import github.jojo.remoting.transport.netty.codec.RpcMessageDecoder;
import github.jojo.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:09
 * @description ---------------【客户端】初始化并且启动客户端 以及 关闭客户端（Bootstrap）-------------
 * 【问题】PipeLine的addLast有顺序要求吗
 */
@Slf4j
public class NettyRpcClient implements RpcRequestTransport {

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    /**
     * 客户端只有一个线程组 【问题】客户端线程数量？
     */
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        //initialize resources such as EventLoopGroup, Bootstrap
        eventLoopGroup = new NioEventLoopGroup();
        //创建客户端引导类
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //设置连接时间 如果超过5s连接还没有建立 那么认为连接失败
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        //当在15s内没有数据发送给服务器(写事件) 那么就发送一个心跳包 客户端-->服务端
                        p.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder());
                        p.addLast(new RpcMessageDecoder());
                        p.addLast(new NettyRpcClientHandler());
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 连接服务端并且返回channel 通过channel可以发送message到服务端
     *
     * @param inetSocketAddress server address
     * @return the channel
     * @SneakyThrows: 将异常向上转为Throwable 然后再利用泛型强转为RuntimeException 编译后泛型都变为了Object 这种方法巧妙的骗过编译器
     * 这样我们就不需要显示的try catch处理异常
     * 【问题】使用CompletableFuture原因?
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        //connect方法是非阻塞且异步的 该方法返回ChannelFuture
        // 1.可以调用Future.get()来等待异步执行结束
        // 2.对此ChannelFuture添加监听器 来回调异步执行的结果 能够更精准把握异步执行结束时间
        //异步的体现:连接操作是在另一个线程中执行的 当前线程添加监听器 addListener方法中：添加完监听器会马上判断监听事件是否done 因此可以更加精准把握时间
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            //监听连接事件
            if (future.isSuccess()) {
                //连接成功
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        //阻塞 直到complete
        return completableFuture.get();
    }

    /**
     * 客户端 发送消息方法 send method
     *
     * @param rpcRequest
     * @return
     */
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        //build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        //build rpc service name by rpcRequest
        //name:interface name+group+version
        String rpcServiceName = rpcRequest.toRpcProperties().toRpcServiceName();
        //get server address
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcServiceName);
        //get server address related channel(connect)
        Channel channel = getChannel(inetSocketAddress);
        //虽然getChannel中判断了channel是否active 这里再进行一次判断
        if (channel.isActive()) {
            //put unprocessed request
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .codec(SerializationTypeEnum.KYRO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            //匿名内部类会隐式的继承一个类或实现一个接口，或者说匿名内部类是一个继承了该类或者实现了该接口的子类匿名对象。
            //------------------------【基础】 注意这里的lambda创建的匿名内部类实际是继承的ChannelFutureListener
            //而非GenericFutureListener
            //原因：ChannelFutureListener extends GenericFutureListener<ChannelFuture> 这样future这个泛型类型确定为了channelFuture
            //future.channel()才能找到该方法
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    //发送失败
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
