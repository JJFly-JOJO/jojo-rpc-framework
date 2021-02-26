package github.jojo.serviceimpl;

import github.jojo.Hello;
import github.jojo.HelloService;
import github.jojo.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 22:54
 * @description
 */
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    static {
        System.out.println("HelloServiceImpl has been built...");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
