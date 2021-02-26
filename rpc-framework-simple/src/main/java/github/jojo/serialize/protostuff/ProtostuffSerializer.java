package github.jojo.serialize.protostuff;

import github.jojo.serialize.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/22 15:45
 * @description
 */
public class ProtostuffSerializer implements Serializer {

    /**
     * 避免每次序列化都重新申请Buffer空间 同时要保证线程安全（这里不再remove 线程复用）
     */
    private final ThreadLocal<LinkedBuffer> bufferThreadLocal = ThreadLocal.withInitial(() -> LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));

    @Override
    public byte[] serialize(Object obj) {
        Class<?> clazz = obj.getClass();
        //Schema 源码本身有缓存 这里不需要使用HashMap缓存Class->Schema
        Schema schema = RuntimeSchema.getSchema(clazz);
        byte[] bytes;
        LinkedBuffer buffer = bufferThreadLocal.get();
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } finally {
            buffer.clear();
        }
        return bytes;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
}
