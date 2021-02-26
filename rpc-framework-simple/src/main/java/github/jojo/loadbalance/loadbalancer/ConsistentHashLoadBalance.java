package github.jojo.loadbalance.loadbalancer;

import github.jojo.loadbalance.AbstractLoadBalance;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 14:54
 * @description ------------一致性hash算法负载均衡策略-------------
 * https://mp.weixin.qq.com/s/ctBM0WO-uWNlTWHI-TPniw
 * https://segmentfault.com/a/1190000021234695
 * 【单例】该对象在运行是只有一个
 */
@Slf4j
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    /**
     * key->serviceName value->server selector
     */
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, String rpcServiceName) {
        //此hashcode用来判断ConsistentHashSelector中缓存的服务器节点是否已经过期（发生了改变）--因为当节点内容发生了增删改事件 此时回调函数会重新创建一个Server list
        int identityHashCode = System.identityHashCode(serviceAddresses);

        ConsistentHashSelector selector = selectors.get(rpcServiceName);

        //检查节点是否更新：
        //如果invokers是一个新的List对象，意味着服务提供者数量发生了变化，可能新增也可能减少了。
        //此时selector.identityHashCode!=identityHashCode条件成立
        //如果是第一次调用此时selector == null条件成立
        if (selector == null || identityHashCode != selector.identityHashCode) {
            //注意这里虚拟节点数我们写死为160个
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            //【线程安全】---------------这里我们并没有先new对象 再使用new的对象的原因是 考虑到并发情况下 可能selectors中的对象并不是刚刚new的对象
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName);
    }

    static class ConsistentHashSelector {

        /**
         * hash环:存放所有服务节点（包括虚拟节点） 使用long原因是 hash环中的值为0~max 不能存在负数
         */
        private final TreeMap<Long, String> virtualInvokers;
        /**
         * 判断节点内容是否发生变化
         */
        private final int identityHashCode;

        /**
         * @param invokers         存放的service节点下所有服务器address
         * @param replicaNumber    每一个服务器产生的虚拟节点个数
         * @param identityHashCode 用于判断节点内容是否发生了变化
         */
        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            //获取每个server address
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    //首先对服务器id port（address）进行md5处理 得到一个长度16的字节数组
                    //i作为编号 进行同一个服务器的区分
                    byte[] digest = md5(invoker + i);
                    //对digest部分字节进行4次hash运算得到四个不同的long型正整数
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        /**
         * h=0时，取digest中下标为0~3的4个字节进行位运算
         * h=1时，取digest中下标为4~7的4个字节进行位运算
         * h=2,h=3时过程同上
         *
         * @param digest
         * @param idx
         * @return
         */
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 |
                    (long) (digest[2 + idx * 4] & 255) << 16 |
                    (long) (digest[1 + idx * 4] & 255) << 8 |
                    (long) (digest[idx * 4] & 255))
                    //最后的与操作的数值可以自己定义 数值的不同可能造成hash值在hash环中分布的均匀性
                    & 4294967295L;
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        /**
         * 这里我们写死了 不考虑调用service方法中要将几个arg参与hash  我们直接选择serviceName参与hash计算 找到落在的treeMap区间 返回server address
         *
         * @param rpcServiceName
         * @return invoker(server address)
         */
        public String select(String rpcServiceName) {
            byte[] digest = md5(rpcServiceName);
            return selectForKey(hash(digest, 0));
        }

        public String selectForKey(long hashCode) {
            //到TreeMap中查找第一个节点值大于或等于当前hashCode的server address(invoker)
            Map.Entry<Long, String> entry = virtualInvokers.ceilingEntry(hashCode);
            if (entry == null) {
                //如果hash大于server address在圆环上最大的位置，此时entry=null，
                //需要将TreeMap的头节点赋值给entry
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }
}
