package github.jojo.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 20:35
 * @description ---------------获取单例对象的工厂类-------------
 */
public class SingletonFactory {

    private static final Map<String, Object> OBJECT_MAP = new HashMap<>();

    private SingletonFactory() {
    }

    /**
     * 【并发】这里要考虑并发情况 因为是handler初始化时会调用！！
     * @param c
     * @param <T>
     * @return
     */
    public static <T> T getInstance(Class<T> c) {
        String key = c.toString();
        Object instance;
        //获取单例对象 考虑并发情况 防止同时创建多个单例
        synchronized (SingletonFactory.class) {
            instance = OBJECT_MAP.get(key);
            if (instance == null) {
                try {
                    instance = c.getDeclaredConstructor().newInstance();
                    OBJECT_MAP.put(key, instance);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    //“||”:   如果左边计算后的操作数为true,右边则不再执行，返回true；
                    //“|”：前后两个操作数都会进行计算。也就是说：“|”不存在短路。
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            return c.cast(instance);
        }
    }

}
