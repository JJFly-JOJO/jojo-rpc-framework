package github.jojo.remoting.transport.netty.client;

import github.jojo.remoting.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:19
 * @description ---------------【客户端】存放服务器还未处理的request 使用CompletableFuture提高性能------------
 */
public class UnprocessedRequests {

    /**
     * key->id value->UnprocessedRequest
     */
    private static final Map<String, CompletableFuture<RpcResponse<Object>>>
            UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future
                = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            //如果返回的response有对应的request 那么将response作为future的完成值 进行赋值
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }

}
