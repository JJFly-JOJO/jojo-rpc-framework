package github.jojo.remoting.handler;

import github.jojo.exception.RpcException;
import github.jojo.factory.SingletonFactory;
import github.jojo.provider.ServiceProvider;
import github.jojo.provider.ServiceProviderImpl;
import github.jojo.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 21:47
 * @description ------------服务端的RpcRequest Processor 用于处理客户端发送来的RpcMessage  是NettyRpcServerHandler 处理request的实现逻辑---------
 */
@Slf4j
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
    }

    /**
     * 处理rpcRequst 调用service实现的serviceImpl方法 并且返回方法执行的结果
     *
     * @param rpcRequest
     * @return
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.toRpcProperties());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 调用service对应的method（method name存放于request中）
     *
     * @param rpcRequest
     * @param service
     * @return the result of the target method execution
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
