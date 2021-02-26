package github.jojo.remoting.transport.netty.client;

import github.jojo.enums.CompressTypeEnum;
import github.jojo.enums.SerializationTypeEnum;
import github.jojo.factory.SingletonFactory;
import github.jojo.remoting.constants.RpcConstants;
import github.jojo.remoting.dto.RpcMessage;
import github.jojo.remoting.dto.RpcResponse;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 20:31
 * @description -----------【客户端】处理服务端发送回来的数据-------------
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 */
@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {

    /**
     * 这两个属性值是由单例工厂产生 也就是说 所有handler都使用的同一对象
     */
    private final UnprocessedRequests unprocessedRequests;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    /**
     * handler执行的主要逻辑部分：处理从服务器端发送来的消息
     *
     * @param ctx
     * @param msg
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            log.info("client receive msg: [{}]", msg);
            //msg是已经经过解码处理后的Object 解码处理后的类型是RpcMessage
            if (msg instanceof RpcMessage) {
                RpcMessage tmp = (RpcMessage) msg;
                //messageType用于区分服务器发送过来的数据是心跳包还是rpc的返回数据
                byte messageType = tmp.getMessageType();
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    //心跳包
                    log.info("heart [{}]", tmp.getData());
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
                    //CompletableFuture 将Response对象设置到此对象中 等待get()方法的调用取出
                    unprocessedRequests.complete(rpcResponse);
                }
            }
        } finally {
            //从InBound里读取的ByteBuf要手动释放，还有自己创建的ByteBuf要自己负责释放。这两处要调用这个release方法。
            //write ByteBuff到OutBound时由netty负责释放，不需要手动调用release
            //调用release实际是让创建的byteBuff引用计数减1
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 监听读写空闲事件 发送心跳包
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                //客户端写空闲 则要发送心跳包了
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                //创建心跳包
                RpcMessage rpcMessage = RpcMessage.builder()
                        //采用Protostuff序列化方法
                        .codec(SerializationTypeEnum.PROTOSTUFF.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .messageType(RpcConstants.HEARTBEAT_REQUEST_TYPE)
                        .data(RpcConstants.PING).build();
                //监听channel的发送事件 如果发送失败 那么关闭channel : ChannelFuture future.channel().close()
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 在处理客户端消息发生异常时处理的逻辑
     *
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        //发生异常 关闭ChannelHandlerContext
        ctx.close();
    }
}
