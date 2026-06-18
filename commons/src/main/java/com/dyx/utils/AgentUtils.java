package com.dyx.utils;

import com.dyx.conf.Properties;
import com.dyx.data.ResponseSchema;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * ReActAgent工具类
 */
@Service
@Slf4j
public class AgentUtils {
    @Resource
    private Properties properties;

    /**
     * 创建ReAct Agent Builder
     */
    public ReActAgent getReActAgentBuilder(String name, String description) {
        String aliApiKey = properties.getAlibabaDashscopeKey();
        String modelName = properties.getModelName();
        log.info("============");
        log.info("Agent使用的大模型: " + modelName);
        log.info("============");
        return ReActAgent.builder().
                name(name)
                .model(DashScopeChatModel.builder()
                        //请求语言大模型的apikey
                        .apiKey(aliApiKey)
                        //所使用的语言大模型
                        .modelName(modelName)
                        //是否流式响应
                        .stream(true)
                        //开启思考模式
                        .enableThinking(true)
                        .build())
                //智能体并发检查
                .checkRunning(true)
                //工具执行超时配置
                .toolExecutionConfig(ExecutionConfig.builder()
                        //工具执行超时 2分钟
                        .timeout(Duration.ofSeconds(120))
                        //最大尝试次数
                        .maxAttempts(1)
                        .build()
                )
                .build();
    }

    /**
     * ReAct Agent流式响应
     */
    public Flux<Event> streamResponse(
            ReActAgent agent,
            String prompt
    ){
        String name = agent.getName();
        //Prompt工具

        PromptUtils promptUtils = new PromptUtils();
        return agent.stream(
                //构建Prompt
                promptUtils.getPrompt(prompt),
                //流式响应配置
                StreamOptions.defaults(),
                //响应格式
                ResponseSchema.class
        );

    }
}
