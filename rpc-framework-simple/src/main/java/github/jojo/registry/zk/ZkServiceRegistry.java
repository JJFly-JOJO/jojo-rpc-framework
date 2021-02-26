package github.jojo.registry.zk;

import github.jojo.registry.ServiceRegistry;
import github.jojo.registry.zk.util.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 21:26
 * @description ------------【zk】基于zookeeper的服务注册类------------
 */
@Slf4j
public class ZkServiceRegistry implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        //inetSocketAddress.toString()= /192.168.49.1:9998
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
