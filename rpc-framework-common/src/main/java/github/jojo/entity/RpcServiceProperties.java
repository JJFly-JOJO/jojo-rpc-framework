package github.jojo.entity;

import lombok.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 21:37
 * @description
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceProperties {
    /**
     * service version
     */
    private String version;
    /**
     * 通过group区分service接口有多个实现类的情况
     */
    private String group;
    private String serviceName;

    public String toRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }
}
