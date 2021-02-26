package github.jojo.loadbalance.loadbalancer;

import github.jojo.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/2/1 17:48
 * @description
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    private final Random random = new Random();

    @Override
    protected String doSelect(List<String> serviceAddresses, String rpcServiceName) {
        int length=serviceAddresses.size();
        int leastActive=-1;
        int leastCount=0;
        int[] leastIndexs=new int[length];
        int totalWeight = 0; // The sum of weights
        int firstWeight = 0; // Initial value, used for comparision
        boolean sameWeight = true; // Every invoker has the same weight value?
        for(int i=0;i<length;i++){

        }
        return null;
    }
}
