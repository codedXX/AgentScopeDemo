package com.dyx;

import com.dyx.utils.AgentUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import java.util.List;

public class QuickModelTest {
//    @Resource
//    private static AgentUtils agentUtils;
//
//    public static void main(String[] args) {
//        // 临时把你的真实 Key 填这里（验证完连同本文件一起删除，别提交 Git）
//        String apiKey = "sk-255506ca196b48f38e686b3e82efac58";
//        // 先用通用名验证连通；通了再换成 .env 里的 qwen3.7-max 复测一次
//        String modelName = "qwen3.7-max";
//
//        ReActAgent reActAgentBuilder = agentUtils.getReActAgentBuilder("quickTest", "测试一下");
//        Flux<Event> result = agentUtils.streamResponse(reActAgentBuilder, "用一句话介绍你自己");
//        System.out.println("结果:"+result);
//    }

    public static void main(String[] args) {
        // 临时把你的真实 Key 填这里（验证完连同本文件一起删除，别提交 Git）
        String apiKey = "sk-255506ca196b48f38e686b3e82efac58";
        // 先用通用名验证连通；通了再换成 .env 里的 qwen3.7-max 复测一次
        String modelName = "qwen3.7-max";

        ReActAgent agent = ReActAgent.builder()
                .name("QuickTest")
                .description("临时验证")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .stream(true)
                        .enableThinking(true)
                        .build())
                .build();

        Msg reply = agent.call(List.of(
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(List.of(TextBlock.builder().text("用一句话介绍你自己").build()))
                        .build()
        )).block();

        System.out.println("==== 大模型功能验证回答 ====");
        System.out.println(reply == null ? "无返回（功能未通）" : reply.getTextContent());
        System.out.println("===========================");
    }
}