package github.jojo.remoting.test.completeble;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:38
 * @description
 */
public class CustomerInfoService implements RemoteLoader {
    @Override
    public String load() {
        System.out.println("CustomerInfoService running...");
        this.delay();
        return "基本信息";
    }
}
