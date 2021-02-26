package github.jojo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/21 22:29
 * @description ---------GZIP压缩类型------
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {

    /**
     *
     */
    GZIP((byte) 0x01, "gzip");

    /**
     *
     */
    private final byte code;
    private final String name;

    /**
     *
     * @param code
     * @return
     */
    public static String getName(byte code) {
        //Enum类和enum关键字定义的类型都有values方法，但是点进去会发现找不到这个方法。
        // 这是因为java编译器在编译这个类（enum关键字定义的类默认继承java.lang.Enum）的时候
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
