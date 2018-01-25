package com.mall.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * 读取配置文件工具类
 * Created by geely
 */
public class PropertiesUtil {

    private static Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static Properties props;

    //tomcat启动的时候就读取里面的内容，所有这里用static静态块来处理
    //静态代码块会在类被加载的时候仅执行一次，一般用其做初始化静态变量
    //执行顺序：静态代码块>普通代码块>构造器代码块
    //Class.forName("com.mysql.jdbc.Driver")作用：注册驱动，把driver加载到jvm虚拟机中，其代码只执行一次
    static {
        String fileName = "mall.properties";
        props = new Properties();
        try {
      //      System.out.println("路径：" + PropertiesUtil.class.getClassLoader());
     //       System.out.println("测试：" + PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName));
            props.load(new InputStreamReader(PropertiesUtil.class.getClassLoader().getResourceAsStream(fileName),"utf-8"));
        } catch (IOException e) {
            logger.error("配置文件读取异常",e);
        }
    }

    //根据key从properties获取value
    public static String getProperty(String key){
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)){
            return null;
        }
        return value.trim();
    }

    public static String getProperty(String key, String defaultValue){
        String value = props.getProperty(key.trim());
        if(StringUtils.isBlank(value)){
            value = defaultValue;
        }
        return value.trim();
    }



}
