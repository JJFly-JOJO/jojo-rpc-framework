package github.jojo.annotation;

import java.lang.annotation.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 15:00
 * @description -------------自定义service注解 用于标注service实现类-----------
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
//@Inherited注解 可以使得父类使用了此注解(@RpcService) 其继承的子类也可以得到该注解
@Inherited
public @interface RpcService {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";
}
