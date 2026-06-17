package com.dyx.conf;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 配置类
 */
@Component
@Getter
public class Properties {

    //大模型名称
    @Value("${agent.model_name}")
    private String modelName;

    //阿里云DashScope Key
    @Value("${agent.alibaba_dashscope_key}")
    private String alibabaDashscopeKey;

    //百度地图MCP服务端地址
    @Value("${mcp.baidu_map_addr}")
    private String baiduMapAddr;

    //LangFuse 服务地址
    @Value("${spring.ai.observation.langfuse.endpoint}")
    private String endpoint;

    //LangFuse 私钥
    @Value("${spring.ai.observation.langfuse.secret-key}")
    private String secretKey;

    //LangFuse 公钥
    @Value("${spring.ai.observation.langfuse.public-key}")
    private String publicKey;
}