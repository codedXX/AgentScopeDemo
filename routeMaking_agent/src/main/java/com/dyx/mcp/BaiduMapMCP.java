package com.dyx.mcp;

import com.dyx.conf.Properties;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class BaiduMapMCP {
    @Resource
    private Properties properties;

    //MCP 客户端
    private McpClientWrapper baiduMapMCP = null;
    //MCP 客户端初始化
    private boolean mcpInitialized = false;

    /**
     * 创建百度地图MCP客户端
     */
    public McpClientWrapper getBaiduMapMCP() {
        //创建MCP客户端
        McpClientWrapper baiduMapMcp= McpClientBuilder.create("BaiduMap-mcp")
                //和MCP Server以SSE方式进行通信
                .sseTransport(properties.getBaiduMapAddr())
                //请求超时
                .timeout(Duration.ofSeconds(120))
                //异步请求
                .buildAsync()
                .block();
        return baiduMapMcp;
    }

    /**
     * 初始化百度地图MCP客户端
     */
    public McpClientWrapper initBaiduMapMCP(McpClientWrapper baiduMapMCP) {

        //通过Optional判断百度MCP客户端是否为null
        Optional<McpClientWrapper> mcpClientWrapper = Optional.ofNullable(baiduMapMCP);
        if(mcpClientWrapper.isPresent()) {
            log.info("==================");
            log.info("百度MCP客户端已经创建");
            log.info("==================");


            if(!mcpInitialized) {
                synchronized (this) {
                    if (!mcpInitialized) {

                        //MCP客户端初始化
                        baiduMapMCP.initialize().block();

                        //获取MCP服务端工具列表
                        if(baiduMapMCP.isInitialized()) {

                            log.info("=============");
                            log.info("百度地图MCP 客户端初始化成功！");
                            log.info("=============");

                            mcpInitialized=true;
                        }

                    }
                }
            }

        }

        return baiduMapMCP;

    }
}
