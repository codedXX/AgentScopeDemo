package com.dyx.utils;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;

/**
 * Nacos 工具类
 */
public class NacosUtil {

    public static AiService getNacosClient() throws NacosException {

        // 设置 Nacos 地址
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "localhost:8848");
        // 创建 Nacos Client
        return AiFactory.createAiService(properties);

    }
}