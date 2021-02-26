package github.jojo.registry;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/28 21:34
 * @description -------------注册模块  发现服务器-----------
 * 注册中心负责服务地址的注册与查找，相当于目录服务。
 * 服务端启动的时候将服务名称及其对应的地址(ip+port)注册到注册中心，
 * 服务消费端根据服务名称找到对应的服务地址。有了服务地址之后，服务消费端就可以通过网络请求服务端了。
 *
 */

import github.jojo.extension.SPI;

import java.net.InetSocketAddress;

/**
 * 服务发现
 */
@SPI
public interface ServiceDiscovery {

    /**
     * 根据rpcServiceName（也就是我们要远程调用的服务接口名称）获取到远程服务的地址（ip+port）
     * @param rpcServiceName 完整的服务名称（class name + group + version）
     * @return 远程服务地址
     */
    InetSocketAddress lookupService(String rpcServiceName);

}
