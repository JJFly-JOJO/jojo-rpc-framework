package github.jojo.remoting.constants;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/2/1 17:11
 * @description
 */
public class ServerConfig {

    /**
     * 服务器权重
     */
    public static String weight = "100";

    public static void setWeight(String w) {
        ServerConfig.weight = w;
    }
}
