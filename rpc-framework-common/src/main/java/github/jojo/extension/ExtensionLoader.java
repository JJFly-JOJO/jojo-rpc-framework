package github.jojo.extension;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/22 19:55
 * @description ------------------SPI拓展机制的实现--------------
 * 核心调用方法：
 * Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
 * 【基础】：final修饰的类不能被继承
 * 【问题】：回顾ClassLoader
 */
@Slf4j
public final class ExtensionLoader<T> {

    /**
     * 统一拓展类配置信息存放的目录
     */
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    /**
     * 所有的拓展实例都是单例 我们将这些拓展实例instance以实现对象的Class(key)->Object(value)缓存
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();
    /**
     * ExtensionLoaders cache
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 拓展接口的类型
     */
    private final Class<?> type;
    /**
     * ExtensionLoader 缓存拓展点接口的所有实例
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /**
     * 缓存拓展接口的所有实现类型Class
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 单例设计模式 构造方法私有化 提供静态方法获取对象
     */
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }
    {}
    /**
     * 根据传入的接口type(Class<S>)获取ExtensionLoader对象
     *
     * @param type
     * @param <S>
     * @return
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        //拓展类型必须有@SPI注解
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        //首先从缓存中查找 如果缓存未命中 那么创建实例
        ExtensionLoader<S> sExtensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (sExtensionLoader == null) {
            //线程安全的put
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            sExtensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return sExtensionLoader;
    }

    /**
     * 根据name获取对应的拓展点（接口）实例
     *
     * @param name kyro=github.javaguide.serialize.kyro.KryoSerializer
     *             protostuff=github.javaguide.serialize.protostuff.ProtostuffSerializer
     *             KYRO((byte) 0x01, "kyro")--->"kyro",
     *             PROTOSTUFF((byte) 0x02, "protostuff")--->"protostuff"
     * @return
     */
    public T getExtension(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        //首先从缓存获取 如果缓存未命中 创建一个对象
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        //创建单实例拓展点实例
        Object instance = holder.get();
        //双重判断 加锁的单例模式
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    //例如:"kyro=github.javaguide.serialize.kyro.KryoSerializer"
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 根据properties文件中的key（name）创建对应的实例instance
     *
     * @param name
     * @return
     */
    private T createExtension(String name) {
        //从配置文件properties中获取Extension维护的type接口类型下的所有拓展类型 再获取指定name的class
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * 获取拓展接口的所有类型Class
     *
     * @return
     */
    private Map<String, Class<?>> getExtensionClasses() {
        //从缓存中获取已经加载了的拓展类型（Class）
        Map<String, Class<?>> classes = cachedClasses.get();
        //double check
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    //将所有的拓展类型加载到Map（目录）中
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 【ClassLoad】加载指定接口名称目录下的所有拓展实现类型Class
     * {@fileName}:统一拓展目录的格式：classpath："META-INF/extensions/"+拓展接口的全限定名称
     *
     * @param extensionClasses
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        //这里我们通知只指定一个固定的目录"META-INF/extensions/"
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();

        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            //------------------获得fileName的完整路径-----------------//
            //【问题】fileName的大小写是否有影响？
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 加载所有拓展的Class
     * 【问题】：文件的IO InputStreamReader
     *
     * @param extensionClasses
     * @param classLoader
     * @param resourceUrl
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            //read every line
            while ((line = reader.readLine()) != null) {
                // get index of comment
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // string after # is comment so we ignore it
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        //我们定义的SPI拓展 严格要求key=value字符串都不能为空
                        if (name.length() > 0 && clazzName.length() > 0) {
                            //Class的创建只会有一个 也就是不存在线程安全问题 不需要使用ConcurrentHashMap 也不需要加锁创建
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
