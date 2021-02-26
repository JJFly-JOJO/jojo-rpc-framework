package github.jojo.serialize.kyro;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import github.jojo.exception.SerializeException;
import github.jojo.remoting.dto.RpcRequest;
import github.jojo.remoting.dto.RpcResponse;
import github.jojo.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/22 14:50
 * @description -----------kryo 效率高 但只支持java语言的序列化----------
 */
public class KryoSerializer implements Serializer {

    /**
     * kryo不是线程安全的 因此每一个线程应该拥有一个kryo实例
     * ---------------ThreadLocal的get()方法-------------
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        //当kryo写一个对象的实例的时候，默认需要将类的完全限定名称写入。
        // 将类名一同写入序列化数据中是比较低效的，所以kryo支持通过类注册进行优化。
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);
        //默认值为true,是否关闭注册行为,关闭之后可能存在序列化问题，一般推荐设置为 true
        kryo.setReferences(true);
        //默认值为false,是否关闭循环引用，可以提高性能，但是一般不推荐设置为 true
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        //括号里的内容支持包括流以及任何可关闭的资源(继承AutoCloseable接口)，
        // 数据流会在 try 执行完毕后自动被关闭，而不用我们手动关闭了
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            //Object->byte:将对象序列化为byte数组
            kryo.writeObject(output, obj);
            //-----------由于线程池的影响 ThreadLocal使用完之后必须remove----------
            //--https://blog.csdn.net/luzhensmart/article/details/86765689---
            kryoThreadLocal.remove();
            return output.toBytes();

        } catch (Exception e) {
            throw new SerializeException("序列化失败...");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            //byte->object:将byte数组反序列为Object
            Object o = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new SerializeException("反序列化失败...");
        }
    }
}
