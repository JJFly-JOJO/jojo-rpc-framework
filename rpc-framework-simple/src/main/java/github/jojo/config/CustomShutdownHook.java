package github.jojo.config;

import github.jojo.registry.zk.util.CuratorUtils;
import github.jojo.remoting.transport.netty.server.NettyRpcServer;
import github.jojo.utils.concurrent.threadpool.ThreadPoolFactoryUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 17:44
 * @description 【服务端】When the server  is closed, do something such as unregister all services
 */
@Slf4j
public class CustomShutdownHook {

    /**
     * 单例模式之饿汉式加载
     */
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    /**
     * JDK提供了Java.Runtime.addShutdownHook(Thread hook)方法，可以注册一个JVM关闭的钩子，
     * 1.这个钩子可以在一下几种场景中被调用：
     * 2.程序正常退出
     * 3.使用System.exit()
     * 4.终端使用Ctrl+C触发的中断
     * 5.系统关闭
     * 6.OutOfMemory宕机
     * 7.使用Kill pid命令干掉进程（注：在使用kill -9 pid时，是不会被调用的）
     */
    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            log.info("clear repository...");
            try {
                InetSocketAddress inetSocketAddress=new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                CuratorUtils.clearRegistry(CuratorUtils.getZkClient(),inetSocketAddress);
            }catch (UnknownHostException ignored){
            }
            //关闭所有的线程池
            //使用挂钩的好处 服务器暴力stop时 为了不让线程池因突然关闭导致线程池中还有未完成的task
            //因此采用优雅的关闭方式 等待task(awaitTermination)完成再shutDown
            ThreadPoolFactoryUtils.shutDownAllThreadPool();
        }));
    }

}
