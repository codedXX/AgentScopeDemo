package com.dyx.agent;

import com.dyx.mcp.BaiduMapMCP;
import com.dyx.utils.AgentUtils;
import com.dyx.utils.ToolUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * description: 路线制定Agent
 */
@Component
@Slf4j
public class RouteMakingAgent {

    @Resource
    private AgentUtils agentUtils;

    @Resource
    private BaiduMapMCP mcp;


    @Bean
    public ReActAgent getRouteMakingAgent() {

        //创建百度地图MCP客户端
        McpClientWrapper baiduMapMCP = mcp.getBaiduMapMCP();
        //初始化百度地图MCP客户端
        McpClientWrapper mcpClient = mcp.initBaiduMapMCP(baiduMapMCP);

        //Toolkit
        ToolUtils toolUtils = new ToolUtils();
        //注册MCP
        Toolkit toolkit = toolUtils.getToolkit(mcpClient);
        // 打印所有工具信息
        toolUtils.getTools();


        //注入到Nacos
        return agentUtils.getReActAgentBuilder(
                "RouteMakingAgent",
                "擅长制定性价比最优的驾车路线"
        )
                .sysPrompt("""
                    你是一个驾车路线制定助手。
                    请调用合适的API接口，
                    制定包括行程距离，高速费用这些方面，
                    性价比最优的路线方案。
                    """)
                //工具包
                .toolkit(toolkit)
                .build();
    }


}