package github.jojo.spring;

import github.jojo.annotation.RpcReference;
import github.jojo.annotation.RpcService;
import github.jojo.entity.RpcServiceProperties;
import github.jojo.extension.ExtensionLoader;
import github.jojo.factory.SingletonFactory;
import github.jojo.provider.ServiceProvider;
import github.jojo.provider.ServiceProviderImpl;
import github.jojo.proxy.RpcClientProxy;
import github.jojo.remoting.transport.RpcRequestTransport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/30 16:06
 * @description ----------自定义后置处理器 主要用来实现远程调用的动态代理----------
 */
@Component
@Slf4j
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class)
                .getExtension("netty");
    }

    /**
     * 作用于服务器端@RpcService注解 在bean对象初始化之前进行注册 且注册到zookeeper中
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            //获取@RpcService注解中的属性值group version
            RpcService rpcServiceAnnotation = bean.getClass().getAnnotation(RpcService.class);
            //创建RpcServiceProperties对象
            RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
                    .group(rpcServiceAnnotation.group()).version(rpcServiceAnnotation.version()).build();
            serviceProvider.publishService(bean, rpcServiceProperties);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        //获取所有字段 查看字段上是否有@RpcReference注解
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceProperties);
                //传入的是接口类型
                Object clientProxy = rpcClientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
