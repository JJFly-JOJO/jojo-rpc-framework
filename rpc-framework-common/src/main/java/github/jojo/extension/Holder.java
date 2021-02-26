package github.jojo.extension;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/22 20:16
 * @description
 */
public class Holder<T> {

    private volatile T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

}
