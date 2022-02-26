package top.shen.ssqmq_server.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

public class PropertiesUtil implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(PropertiesUtil.class);

    /**
     * 根据配置文件的key返回配置文件的值
     * @param key yml或者properties 配置文件的key
     * @return
     */
    /**
     * 根据配置文件的key返回配置文件的值
     * @param key yml或者properties 配置文件的key
     * @return
     */
    public static String getProperties(String key) {


        try {
            Yaml yaml = new Yaml();
            //先读取yml文件
            Map<String,Object> map = yaml.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("application.yml"));
            String [] keyArr = key.split("\\.");
            for (int i = 0; i < keyArr.length; i++) {

                if (i == keyArr.length-1) {
                    return map.get(keyArr[i]).toString();
                } else {
                    map = (Map<String,Object>) map.get(keyArr[i]);
                }
            }
        } catch (Exception e) {
            //读取properties文件失败 在读取properties文件
            try {
                Properties ps = new Properties();
                ps.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties"));
                return ps.getProperty(key);
            } catch (Exception e2) {
                log.error("read config fail");
            }
        }
        return null;
    }

}
