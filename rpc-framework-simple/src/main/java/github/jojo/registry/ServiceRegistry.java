package github.jojo.registry;

import github.jojo.extension.SPI;

import java.net.InetSocketAddress;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 21:24
 * @description -----------服务注册----------
 */
@SPI
public interface ServiceRegistry {

    /**
     * register service
     *
     * @param rpcServiceName    rpc service name
     * @param inetSocketAddress service address
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

}
