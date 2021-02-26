package github.jojo.remoting.test.funcinterface;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/29 17:14
 * @description
 */
public interface FunctionMethod<T extends Object> {

    void test(T a);
}
