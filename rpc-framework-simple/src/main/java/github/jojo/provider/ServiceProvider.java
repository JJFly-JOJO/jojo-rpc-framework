package github.jojo.provider;

import github.jojo.entity.RpcServiceProperties;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 17:39
 * @description --------------服务service提供者 用于储存且提供service object------------
 */
public interface ServiceProvider {

    /**
     * @param service              service object
     * @param serviceClass         the interface class implemented by the service instance object
     * @param rpcServiceProperties service related attributes
     */
    void addService(Object service, Class<?> serviceClass, RpcServiceProperties rpcServiceProperties);

    /**
     * @param rpcServiceProperties service related attributes
     * @return service object
     */
    Object getService(RpcServiceProperties rpcServiceProperties);

    /**
     * @param service              service object
     * @param rpcServiceProperties service related attributes
     */
    void publishService(Object service, RpcServiceProperties rpcServiceProperties);

    /**
     * @param service service object
     */
    void publishService(Object service);

}
