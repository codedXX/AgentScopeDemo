package com.dyx.utils;

import com.dyx.conf.Properties;
import com.dyx.data.ResponseSchema;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * description: ReActAgent 工具类
 */
@Service
@Slf4j
public class AgentUtils {

    @Resource
    private Properties properties;


    //===========
    // 从配置中读取敏感资源, 不能使用static
    //=============



    /**
     * author: Imooc
     * description: 创建ReAct Agent Builder
     * @param name:
     * @param description:
     * @return io.agentscope.core.ReActAgent.Builder
     */
    public ReActAgent.Builder getReActAgentBuilder(
            String name,
            String description
    ) {


        String aliApiKey = properties.getAlibabaDashscopeKey();
        String modelName = properties.getModelName();
        log.info("============");
        log.info("Agent使用的大模型: "+modelName);
        log.info("============");


        return ReActAgent.builder()
                        .name(name)
                        .description(description)
                        //大模型配置
                        .model(
                                DashScopeChatModel.builder()
                                        //请求语言大模型的apikey
                                        .apiKey(aliApiKey)
                                        //所使用的语言大模型
                                        .modelName(modelName)
                                        //是否流式响应
                                        .stream(true)
                                        //开启思考模式
                                        .enableThinking(true)
                                        .build()
                        )
                        //智能体并发检查
                        .checkRunning(true)
                        //工具执行超时配置
                        .toolExecutionConfig(ExecutionConfig.builder()
                                //工具执行超时 5分钟 (远程A2A Agent是完整ReAct+思考+地图MCP, 2分钟常不够)
                                .timeout(Duration.ofSeconds(300))
                                //最大尝试次数
                                .maxAttempts(1)
                                .build())

                ;

    }

    /**
     * author: Imooc
     * description: ReAct Agent 流式响应
     * @param agent:
     * @param prompt:
     * @return reactor.core.publisher.Flux<io.agentscope.core.agent.Event>
     */
    public Flux<Event> streamResponse(
            AgentBase agent,
            String prompt) {

        String name = agent.getName();
        //Prompt工具
        PromptUtils promptUtils =  new PromptUtils();

        try {

            return agent.stream(
                    //构建Prompt
                    promptUtils.getPrompt(prompt)
                    ,
                    //流式响应配置
                    StreamOptions.defaults(),
                    //响应格式
                    ResponseSchema.class
            );

        }catch (Exception e) {
            log.error("===================");
            log.error(name+"  正在忙......");
            log.error(e.getMessage());
            log.error("===================");

            return null;
        }

    }


}