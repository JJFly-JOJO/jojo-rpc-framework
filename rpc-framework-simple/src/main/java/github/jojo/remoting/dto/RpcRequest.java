package github.jojo.remoting.dto;

import github.jojo.entity.RpcServiceProperties;
import lombok.*;

import java.io.Serializable;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 21:01
 * @description ---------------【客户端】发送的请求指明接口 要调用哪一个接口实现类 带来函数参数等--------------
 * @RpcReference(version = "version1", group = "test1")
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {

    /**
     * UID号为后续不兼容升级提供可能
     */
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    /**
     * group用于处理一个接口有多个实现类
     */
    private String group;

    /**
     * 使用builder创建RpcServiceProperties对象并且对属性赋值
     *
     * @return
     */
    public RpcServiceProperties toRpcProperties() {
        return RpcServiceProperties.builder().serviceName(this.getInterfaceName())
                .version(this.getVersion())
                .group(this.getGroup()).build();
    }

}
