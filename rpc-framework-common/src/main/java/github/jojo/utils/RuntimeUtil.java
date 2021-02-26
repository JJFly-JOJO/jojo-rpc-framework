package github.jojo.utils;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 20:33
 * @description
 */
public class RuntimeUtil {

    /**
     * 获取cpu核心数
     * @return cpu核心数
     */
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
