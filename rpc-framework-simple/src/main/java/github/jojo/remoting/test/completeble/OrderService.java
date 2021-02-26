package github.jojo.remoting.test.completeble;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 15:24
 * @description
 */
public class OrderService implements RemoteLoader {

    @Override
    public String load() {
        System.out.println("OrderService running...");
        this.delay();
        return "订单信息";
    }

}
