package github.jojo.test.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/2/22 0:36
 * @description
 */
public class TestBuffer {

    @Test
    public void testBuffer() {
        ByteBuf buffer = Unpooled.buffer(40);
        buffer.writeBytes("Netty".getBytes());
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.discardReadBytes();
    }

}
