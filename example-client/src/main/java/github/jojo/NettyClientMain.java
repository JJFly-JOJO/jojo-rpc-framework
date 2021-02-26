package github.jojo;

import github.jojo.annotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 15:23
 * @description ----------------客户端启动--------------
 */
@RpcScan(basePackage = {"github.jojo"})
public class NettyClientMain {

    public static void main(String[] args) throws InterruptedException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyClientMain.class);
        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
        helloController.test();
    }

}
