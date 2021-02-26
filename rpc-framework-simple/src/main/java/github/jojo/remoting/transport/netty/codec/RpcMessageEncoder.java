package github.jojo.remoting.transport.netty.codec;

import github.jojo.compress.Compress;
import github.jojo.enums.CompressTypeEnum;
import github.jojo.enums.SerializationTypeEnum;
import github.jojo.extension.ExtensionLoader;
import github.jojo.remoting.constants.RpcConstants;
import github.jojo.remoting.dto.RpcMessage;
import github.jojo.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 22:07
 * @description -----------------自定义传输协议==自定义编码器 负责处理“出站”消息，将消息格式转换为字节数组然后写入到ByteBuf对象中------------
 * <p>
 * * <pre>
 *  *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *  *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *  *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *  *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *  *   |                                                                                                       |
 *  *   |                                         body                                                          |
 *  *   |                                                                                                       |
 *  *   |                                        ... ...                                                        |
 *  *   +-------------------------------------------------------------------------------------------------------+
 *  * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 *  * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 *  * body（object类型数据）
 *  * </pre>
 * <p>
 * 魔法数magic code ： 通常是 4 个字节。这个魔数主要是为了筛选来到服务端的数据包，有了这个魔数之后，服务端首先取出前面四个字节进行比对，
 * 能够在第一时间识别出这个数据包并非是遵循自定义协议的，也就是无效数据包，为了安全考虑可以直接关闭连接以节省资源。
 * 序列化器类型 ：标识序列化的方式，比如是使用 Java 自带的序列化，还是 json，kyro 等序列化方式。
 * 消息长度 ： 运行时计算出来。
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    /**
     * 将RpcMessage编码成字节数组输出
     * 注意这里 RpcMessage是整段放入ByteBuf中输出的，没有拆包，直接传输
     * @param ctx
     * @param rpcMessage
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            //首先是4B魔数magic code 4B
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            //版本version 1B
            out.writeByte(RpcConstants.VERSION);
            //消息长度 为int类型 占四个字节 因此将当前byte下标后移4位 4B
            out.writerIndex(out.writerIndex() + 4);
            //消息类型（心跳包 请求包 响应包）1B
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            //序列化类型 1B
            out.writeByte(rpcMessage.getCodec());
            //压缩类型 byte-->String("gzip") 1B
            out.writeByte(CompressTypeEnum.GZIP.getCode());
            //请求ID 并发环境下 使用原子自增Atomic 4B
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            //请求体body的序列化
            byte[] bodyBytes = null;
            //请求头长度16B
            int fullLength = RpcConstants.HEAD_LENGTH;
            //如果请求不是心跳包 那么请求长度fullLength= head length + body length
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                //序列化rpcMessage中的data（封装的是request或response）
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                //进一步优化 GZIP压缩技术
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            //数据长度fullLength最终才能确定
            int writeIndex = out.writerIndex();
            //1 为版本version 1B
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            //reset index
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
