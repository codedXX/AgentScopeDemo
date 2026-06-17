package com.dyx.utils;

import com.dyx.conf.Properties;
import io.agentscope.core.tracing.telemetry.TelemetryTracer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * description: LangFuse Agent观测 工具类
 */

@Service
@Slf4j
public class LangFuseUtils {


    @Resource
    private Properties properties;

    //初始化 LangFuse Agent观测
    public TelemetryTracer initLangfuseTracing() {

        String endpoint = properties.getEndpoint();
        String authHeader = getAuthHeader();

        // 创建TelemetryTracer并配置Langfuse
        TelemetryTracer langfuseTracer = TelemetryTracer.builder()
                .endpoint(endpoint)
                .addHeader("Authorization", authHeader)
                .build();

        return langfuseTracer;

    }

    /**
     * author: Imooc
     * description: 构建LangFuse的认证头
     * @param :
     * @return java.lang.String
     */
    private String getAuthHeader() {

        String publicKey = properties.getPublicKey();
        String secretKey = properties.getSecretKey();

        String credentials = publicKey + ":" + secretKey;
        String authHeader = "Basic " +
                Base64.getEncoder().encodeToString(credentials.getBytes());

        return authHeader;

    }
}