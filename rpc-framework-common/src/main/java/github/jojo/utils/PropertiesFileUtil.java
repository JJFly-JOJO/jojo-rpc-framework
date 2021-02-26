package github.jojo.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author zzj
 * @version 1.0
 * @date 2021/1/28 23:06
 * @description -----------读取properties属性文件的工具类-----------
 */
@Slf4j
public class PropertiesFileUtil {

    private PropertiesFileUtil() {
    }

    @SneakyThrows
    public static Properties readPropertiesFile(String fileName) {
        //技巧---------------获取类路径 ContextClassLoader:默认情况下是AppClassLoader(SystemClassLoader)
        //得到的类加载器的路径为:classpath(target)/classes/
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath = "";
        if (url != null) {
            rpcConfigPath = url.getPath() + fileName;
        }
        //中文编码环境下的空格（" " = 20%）问题
        rpcConfigPath = URLDecoder.decode(rpcConfigPath, "UTF-8");
        Properties properties = null;
        try (InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)) {
            properties = new Properties();
            properties.load(inputStreamReader);
        } catch (IOException e) {
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }

}
