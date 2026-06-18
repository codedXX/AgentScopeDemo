package com.dyx.utils;

import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * Agent Tool工具类
 */
@Slf4j
public class ToolUtils {
    private final Toolkit toolkit;

    public ToolUtils() {
        //创建工具包
        toolkit=new Toolkit();
    }

    /**
     * 获取工具包
     */
    public Toolkit getToolkit(Object tool)
    {
        //把工具添加到工具包，能自动扫描@Tool所注释的方法，作为Agent的工具
        toolkit.registerTool(tool);
        return toolkit;
    }

    /**
     * 获取工具包
     */
    public Toolkit getToolkit(McpClientWrapper mcp)
    {
        //把MCP服务端的所有工具添加到工具包
        toolkit.registerMcpClient(mcp).block();
        return toolkit;
    }

    /**
     * 获取所有工具信息
     */
    public void getTools(){
        log.info("========= 已加载的工具 ==========");
        List<ToolSchema> tools = toolkit.getToolSchemas();
        for (ToolSchema tool : tools) {
            log.info("工具: " + tool.getName());
            log.info("描述: " + tool.getDescription());
//            log.info("参数: " + tool.getParameters());
            log.info("---------------");
        }
        log.info("===================");
    }

}
