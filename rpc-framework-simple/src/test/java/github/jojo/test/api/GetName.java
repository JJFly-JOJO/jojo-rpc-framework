package github.jojo.test.api;

import github.jojo.serialize.Serializer;
import org.junit.Test;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/31 21:39
 * @description
 */
public class GetName {

    @Test
    public void test() {
        System.out.println(Serializer.class.getCanonicalName());
        System.out.println(Serializer.class.getName());
    }

}
