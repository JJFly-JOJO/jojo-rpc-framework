package github.jojo.annotation;

import java.lang.annotation.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 15:33
 * @description --------用于客户端service接口字段标注----------
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {
    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";
}
