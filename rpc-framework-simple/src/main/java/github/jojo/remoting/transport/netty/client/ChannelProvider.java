package github.jojo.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 17:20
 * @description ----------【客户端】用于存放、获取客户端与每一个服务器的连接（SocketChannel）---------------
 * 【question】:会同时出现两个一样ip:port的连接吗？
 */
@Slf4j
public class ChannelProvider {

    /**
     * key->server ip value->channel
     */
    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取channel（一个socket套接字）
     * InetSocketAddress维护了服务端的IP和port:在分布式rpc情况下 会有多个服务器提供远程调用 也就是一个客户端与多个服务端连接
     *
     * @param inetSocketAddress
     * @return
     */
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.getHostString();
        //判断是否存在一个连接（channel）对应当前服务器的ip port
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            //如果存在 判断当前连接是否可用（与服务器端是否还是连接着的）
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }

}
