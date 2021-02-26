package github.jojo;

import github.jojo.annotation.RpcScan;
import github.jojo.entity.RpcServiceProperties;
import github.jojo.remoting.constants.ServerConfig;
import github.jojo.remoting.transport.netty.server.NettyRpcServer;
import github.jojo.serviceimpl.HelloServiceImpl2;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 22:51
 * @description ---------服务器启动 通过@RpcService 自动注册service服务
 */
@RpcScan(basePackage = {"github.jojo"})
public class NettyServerMain {
    public static void main(String[] args) {
        ServerConfig.setWeight("50");
        // Register service via annotation
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        // Register service manually
        HelloService helloService2 = new HelloServiceImpl2();
        RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
                .group("test2").version("version2").build();
        nettyRpcServer.registerService(helloService2, rpcServiceProperties);
        nettyRpcServer.start();
    }
}
