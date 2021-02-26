package github.jojo.remoting.transport;

import github.jojo.extension.SPI;
import github.jojo.remoting.dto.RpcRequest;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 15:52
 * @description ----------SPI机制 客户端可以选择传统socket 也可以选择基于netty send message---------
 */
@SPI
public interface RpcRequestTransport {

    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
