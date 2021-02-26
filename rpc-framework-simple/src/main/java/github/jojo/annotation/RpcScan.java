package github.jojo.annotation;

import github.jojo.spring.CustomScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 17:55
 * @description
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
//Import不管嵌套多少层 都能在AppCtx创建容器时扫描到
@Import(CustomScannerRegistrar.class)
@Documented
public @interface RpcScan {

    String[] basePackage();

}
