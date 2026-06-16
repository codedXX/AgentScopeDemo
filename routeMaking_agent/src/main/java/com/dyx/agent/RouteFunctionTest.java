package com.dyx.agent;

import com.dyx.utils.PromptUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
//public class RouteFunctionTest implements CommandLineRunner {
public class RouteFunctionTest{

    // 注入第 4.5 节 @Bean 方法产出的路线 Agent（Bean 名就是方法名 getRouteMakingAgent）
    @Resource(name = "getRouteMakingAgent")
    private ReActAgent routeAgent;

//    @Override
//    public void run(String... args) {
//        PromptUtils promptUtils = new PromptUtils();
//        Msg reply = routeAgent.call(List.of(
//                promptUtils.getPrompt("帮我规划从深圳市到惠州市的自驾路线，给出大致里程和高速过路费")
//        )).block();
//
//        System.out.println("======= 路线Agent功能验证回答 =======");
//        System.out.println(reply == null ? "无返回" : reply.getTextContent());
//        System.out.println("====================================");
//    }
}