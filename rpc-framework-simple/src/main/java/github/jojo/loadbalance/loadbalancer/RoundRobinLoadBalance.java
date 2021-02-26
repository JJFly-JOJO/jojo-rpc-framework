package github.jojo.loadbalance.loadbalancer;

import github.jojo.loadbalance.AbstractLoadBalance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/2/1 18:09
 * @description ---------轮询负载均衡策略--------
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    /**
     * 记录所有提供服务的数据（记录服务(serverAddr+service)调用的次数(不是具体到哪个server的调用次数 是整体方法调用次数)）
     */
    private final ConcurrentMap<String, AtomicInteger> sequences = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, String rpcServiceName) {
        String key = rpcServiceName;
        int length = serviceAddresses.size();
        int maxWeight = 0;
        int minWeight = Integer.MAX_VALUE;
        //记录提供service的服务器对应的每个权重
        LinkedHashMap<String, IntegerWrapper> serverToWeightMap = new LinkedHashMap<>();
        int weightSum = 0;
        for (String serviceAddress : serviceAddresses) {
            int weight = getWeight(rpcServiceName, serviceAddress);
            maxWeight = Math.max(maxWeight, weight);
            minWeight = Math.min(minWeight, weight);
            if (weight > 0) {
                //只添加有权重的实例
                serverToWeightMap.put(serviceAddress, new IntegerWrapper(weight));
                weightSum += weight;
            }
        }
        AtomicInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicInteger());
            sequence = sequences.get(key);
        }
        //实例调用次数+1
        int currentSequence = sequence.getAndIncrement();
        //实例有权重 且权重值不同 则根据权重大小分配
        if (maxWeight > 0 && minWeight < maxWeight) {
            //调用次数%权重数 得到偏移量mod
            int mod = currentSequence % weightSum;
            // 遍历最大的权重值，
            // 为什么会遍历它?
            // 因为每一次循环就遍历所有的实例，一个实例最大的权重为 maxWeight，
            // 最多遍历 maxWeight 次所有实例就可以找到想要的实例
            for (int i = 0; i < maxWeight; i++) {
                for (Map.Entry<String, IntegerWrapper> each : serverToWeightMap.entrySet()) {
                    String k = each.getKey();
                    IntegerWrapper v = each.getValue();
                    if (mod == 0 && v.getValue() > 0) {
                        return k;
                    }
                    if (v.getValue() > 0) {
                        //实例没有选中 权重-1
                        v.decrement();
                        mod--;
                    }
                }
            }
        }
        //如果没有权重 或者权重数值相等 则调用次数%server数量 得到对应下标
        return serviceAddresses.get(currentSequence % length);
    }

    private static final class IntegerWrapper {
        private int value;

        public IntegerWrapper(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void decrement() {
            this.value--;
        }
    }
}
