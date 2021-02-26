package github.jojo.loadbalance;

import github.jojo.registry.zk.util.CuratorUtils;

import java.util.List;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 13:25
 * @description ------------此抽象类实现接口逻辑 将一些共同的处理的逻辑代码放在此方法中 再提供出一个抽象方法 由子类继承实现具体的算法策略------------
 */
public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceAddress, String rpcServiceName) {
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return null;
        }
        if (serviceAddress.size() == 1) {
            return serviceAddress.get(0);
        }
        return doSelect(serviceAddress, rpcServiceName);
    }

    /**
     * 由向下继承的子类实现 这是一个拓展点
     *
     * @param serviceAddresses
     * @param rpcServiceName
     * @return
     */
    protected abstract String doSelect(List<String> serviceAddresses, String rpcServiceName);

    /**
     * 获取当前服务器权重
     *
     * @param rpcServiceName
     * @return
     */
    protected int getWeight(String rpcServiceName, String serviceAddresses) {
        Object v = CuratorUtils.getNodeValue(CuratorUtils.getZkClient(), rpcServiceName, serviceAddresses);
        if (v == null) {
            return -1;
        }
        return Integer.parseInt(new String((byte[]) v));
    }
}
