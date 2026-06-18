package com.dyx;

import com.dyx.utils.AgentUtils;
import com.dyx.utils.PromptUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class RouteFunctionTest implements CommandLineRunner {
    @Resource(name = "getRouteMakingAgent")
    private ReActAgent routeAgent;
    @Resource
    private AgentUtils agentUtils;

    @Override
    public void run(String... args) throws Exception {
        PromptUtils promptUtils = new PromptUtils();
//        Msg reply = routeAgent.call(promptUtils.getPrompt("帮我规划从深圳市到惠州市的自驾路线，给出大致里程和高速过路费")).block();
//        System.out.println("======= 路线Agent功能验证回答 =======");
//        System.out.println(reply == null ? "无返回" : reply.getTextContent());
//        System.out.println("====================================");
        Flux<Event> eventFlux = agentUtils.streamResponse(routeAgent, "帮我规划从深圳市到惠州市的自驾路线，给出大致里程和高速过路费");
        eventFlux
                .doOnNext(event -> {
                    Msg msg = event.getMessage();
                    if (msg != null) {
                        System.out.print(msg.getTextContent());   // 用 print 不换行 → 像打字机一段段出
                    }
                })
                .blockLast();   // ★ 关键:阻塞当前线程直到流结束
    }
}
