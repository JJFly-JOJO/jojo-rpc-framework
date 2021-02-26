package github.jojo.remoting.dto;

import lombok.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 21:00
 * @description ----------实际封装的数据-------
 * Request对象封装在 RpcMessage的data中----(encoder)---->byte数组 发送给服务器
 * Response对象封装在 RpcMessage的data中<----(decoder)----客户端接收的数据
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {
    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id
     */
    private int requestId;
    /**
     * response or request data
     */
    private Object data;
}
