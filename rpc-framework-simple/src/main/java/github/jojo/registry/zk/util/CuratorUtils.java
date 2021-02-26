package github.jojo.registry.zk.util;

import github.jojo.enums.RpcConfigEnum;
import github.jojo.remoting.constants.ServerConfig;
import github.jojo.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/28 22:25
 * @description ------------Curator(zookeeper client) utils-----------
 */
@Slf4j
public class CuratorUtils {

    /**
     * 服务发现节点都注册到/my-rpc下
     */
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    /**
     * 重试之间等待的初始时间
     */
    private static final int BASE_SLEEP_TIME = 1000;
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRIES = 3;
    /**
     * key->rpcServiceName value->the server nodes under the service node
     */
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Object> SERVICE_NODE_VALUE = new ConcurrentHashMap<>();
    /**
     * 【ConcurrentHashMap】
     */
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    /**
     * 单例模式 只要一个zkClient实例
     */
    private static CuratorFramework zkClient;

    private CuratorUtils() {
    }

    /**
     * 创建持久化的节点（注意既然是持久化节点 当服务器关闭后就需要手动删除节点）
     * Unlike temporary nodes, persistent nodes are not removed when the client disconnects
     *
     * @param "/my-rpc" + "/" + rpcServiceName + inetSocketAddress.toString()
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                //creatingParentsIfNeeded() 可以保证父节点不存在的时候自动创建父节点
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                zkClient.setData().forPath(path, ServerConfig.weight.getBytes());
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取某个节点下的所有子节点(节点中存放的是服务器的url)
     *
     * @param zkClient
     * @param rpcServiceName 节点的名称为对外暴露的服务（service）
     * @return
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        //首先看缓存中是否存在
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 获取节点值(weight)
     *
     * @param zkClient
     * @param rpcServiceName
     */
    public static Object getNodeValue(CuratorFramework zkClient, String rpcServiceName, String serverAddress) {
        Object result = null;
        if (!SERVICE_ADDRESS_MAP.containsKey(rpcServiceName) || !SERVICE_ADDRESS_MAP.get(rpcServiceName).contains(serverAddress)) {
            return result;
        }
        String key = serverAddress;
        if (SERVICE_NODE_VALUE.containsKey(key)) {
            result = SERVICE_NODE_VALUE.get(key);
        } else {
            String path = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + "/" + serverAddress;
            try {
                result = zkClient.getData().forPath(path);
                SERVICE_NODE_VALUE.put(key, result);
                result = SERVICE_NODE_VALUE.get(key);
            } catch (Exception e) {
                log.error("get value of the node for path [{}] fail", path);
            }
        }
        return result;
    }

    /**
     * 【服务端】当服务器关闭时 需要注销zookeeper中注册的节点
     *
     * @param zkClient
     * @param inetSocketAddress
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        //并行流的使用(并发执行)
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    /**
     * 创建zookeeper客户端 并且连接到服务端的zookeeper
     *
     * @return
     */
    public static CuratorFramework getZkClient() {
        //检查用户是否自定义了属性配置文件 配置了zookeeper地址
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ?
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        //如果zkClient已经连接 那么直接返回
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        //连接策略 尝试3次 每次尝试间隔为1s
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                //连接的zookeeper服务端地址
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        return zkClient;
    }

    /**
     * 【客户端】对具体的节点添加监听器 监听节点中内容发生变化
     * 注册了监听器之后，这个节点的子节点发生变化比如增加、减少或者更新的时候，你可以自定义回调操作。
     *
     * @param rpcServiceName
     * @param zkClient
     */
    public static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        //This class(PathChildrenCache) will watch the ZK path, respond to update/create/delete events, pull down the data
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            //----------------------------【更新】-----------------------------//
            //这里直接覆盖掉之前缓存的服务器list 对应的 hash值也发生了变化 使得后面的负载均衡selector可以发现节点发生了变化
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
            //清空之前所有节点value缓存值
            SERVICE_NODE_VALUE.clear();
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
