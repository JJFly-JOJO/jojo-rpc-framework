package github.jojo.serviceimpl;

import github.jojo.Hello;
import github.jojo.HelloService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 22:56
 * @description
 */
@Slf4j
public class HelloServiceImpl2 implements HelloService {

    static {
        System.out.println("HelloServiceImpl2被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl2收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl2返回: {}.", result);
        return result;
    }

}
