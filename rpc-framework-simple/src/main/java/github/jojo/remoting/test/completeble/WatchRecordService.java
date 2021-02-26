package github.jojo.remoting.test.completeble;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 15:24
 * @description
 */
public class WatchRecordService implements RemoteLoader {
    @Override
    public String load() {
        System.out.println("WatchRecordService running...");
        this.delay();
        return "观看记录";
    }
}
