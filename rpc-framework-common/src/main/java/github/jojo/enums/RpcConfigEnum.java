package github.jojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 12:35
 * @description --------------zookeeper 属性配置文件路径(配置zookeeper ip port)-------------
 */
@AllArgsConstructor
@Getter
public enum RpcConfigEnum {

    RPC_CONFIG_PATH("rpc.properties"),
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;

}
