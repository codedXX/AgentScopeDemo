package com.dyx.agents;

import com.dyx.utils.PromptUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
//public class TripFunctionTest implements CommandLineRunner {
public class TripFunctionTest {

    // 注入第 5.5 节 @Bean 方法产出的行程 Agent（Bean 名就是方法名 getTripPlannerAgent）
    @Resource(name = "getTripPlannerAgent")
    private ReActAgent tripAgent;

//    @Override
//    public void run(String... args) {
//        PromptUtils promptUtils = new PromptUtils();
//        Msg reply = tripAgent.call(List.of(
//                promptUtils.getPrompt("推荐惠州值得去的景点、当地特色美食和高性价比住宿")
//        )).block();
//
//        System.out.println("======= 行程Agent功能验证回答 =======");
//        System.out.println(reply == null ? "无返回" : reply.getTextContent());
//        System.out.println("====================================");
//    }
}