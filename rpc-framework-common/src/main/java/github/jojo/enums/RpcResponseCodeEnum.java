package github.jojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:01
 * @description -----------RpcResponse对象中用到的一些常量-----------
 */
@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {

    /**
     * success or fail to RpcResponse
     */
    SUCCESS(200, "The remote call is successful"),
    FAIL(500, "The remote call is fail");

    private final int code;

    private final String message;

}
