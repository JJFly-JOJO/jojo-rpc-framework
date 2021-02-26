package github.jojo.compress;

import github.jojo.extension.SPI;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/22 22:14
 * @description -----------Extension 拓展的压缩接口----------
 */
@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
