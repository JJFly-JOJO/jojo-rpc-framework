package github.jojo.remoting.test.completeble;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 15:24
 * @description
 */
public class LabelService implements RemoteLoader {
    @Override
    public String load() {
        System.out.println("LabelService running...");
        this.delay();
        return "标签信息";
    }
}
