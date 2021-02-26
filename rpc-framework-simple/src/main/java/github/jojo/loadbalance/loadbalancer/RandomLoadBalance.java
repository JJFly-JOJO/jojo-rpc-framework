package github.jojo.loadbalance.loadbalancer;

import github.jojo.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 16:12
 * @description ----------随机负载均衡算法--------
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    /**
     * random本身是线程安全的 但是高并发情况下性能低下（next()函数中的CAS 造成许多线程在自旋重试）
     */
    private final Random random = new Random();

    @Override
    protected String doSelect(List<String> serviceAddresses, String rpcServiceName) {
        int length = serviceAddresses.size();
        int totalWeight = 0;
        //标识每个被调用服务的实例权重是否相同
        boolean sameWeight = true;
        for (int i = 0; i < length; i++) {
            int weight = getWeight(rpcServiceName, serviceAddresses.get(i));
            totalWeight += weight;
            if (sameWeight && i > 0 && weight != getWeight(rpcServiceName, serviceAddresses.get(i - 1))) {
                //有不同的权重值
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            int offset = random.nextInt(totalWeight);
            for (String serviceAddress : serviceAddresses) {
                offset -= getWeight(rpcServiceName, serviceAddress);
                if (offset < 0) {
                    return serviceAddress;
                }
            }
        }
        //如果所有权重都相同 则随机产生一个0~size-1的数
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
