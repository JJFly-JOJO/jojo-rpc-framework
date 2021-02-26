package github.jojo.remoting.test.completeble;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/20 22:37
 * @description
 */
public interface RemoteLoader {
    String load();

    default void delay() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
