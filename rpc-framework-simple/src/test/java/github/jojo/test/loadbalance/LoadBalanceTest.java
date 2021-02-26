package github.jojo.test.loadbalance;

import github.jojo.extension.ExtensionLoader;
import github.jojo.loadbalance.LoadBalance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/2/1 21:08
 * @description
 */
public class LoadBalanceTest {
    @Test
    void TestConsistentHashLoadBalance() {
        LoadBalance loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
        List<String> serviceUrlList = new ArrayList<>(Arrays.asList("127.0.0.1:9997", "127.0.0.1:9998", "127.0.0.1:9999"));
        String userRpcServiceName = "github.javaguide.UserServicetest1version1";
        String userServiceAddress = loadBalance.selectServiceAddress(serviceUrlList, userRpcServiceName);
        assertEquals("127.0.0.1:9999",userServiceAddress);
        String schoolRpcServiceName = "github.javaguide.SchoolServicetest1version1";
        String schoolServiceAddress = loadBalance.selectServiceAddress(serviceUrlList, schoolRpcServiceName);
        assertEquals("127.0.0.1:9997",schoolServiceAddress);
    }
}
