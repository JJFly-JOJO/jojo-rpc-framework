package github.jojo.loadbalance;

import github.jojo.extension.SPI;

import java.util.List;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/28 21:54
 * @description -------------负载均衡策略------------
 */
@SPI
public interface LoadBalance {

    /**
     * 从当前所有的服务器列表（list）中选择一个服务器
     * @param serviceAddress 服务器列表
     * @param rpcServiceName 服务器对外暴露的服务
     * @return 返回选择的目标服务器
     */
    String selectServiceAddress(List<String> serviceAddress, String rpcServiceName);

}
