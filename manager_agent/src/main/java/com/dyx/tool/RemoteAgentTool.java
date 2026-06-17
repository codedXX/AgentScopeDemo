package com.dyx.tool;

import com.alibaba.nacos.api.exception.NacosException;
import com.dyx.utils.AgentUtils;
import com.dyx.utils.NacosUtil;
import com.dyx.utils.PromptUtils;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * description: 将远程Agent封装为工具
 */

@Service
@Slf4j
public class RemoteAgentTool {

    @Resource
    private AgentUtils agentUtils;

    /**
     * author: Imooc
     * description: 基于A2A协议获取路线制定Agent
     * @param :
     * @return void
     */
    @Tool(description = "擅长制定最优驾车路线")
    public String callRouteMakingAgent(
            //工具参数
            @ToolParam(name = "prompt", description = "驾车的起点和终点")
            String prompt) throws NacosException {

        log.info("============");
        log.info("工具方法：路线制定智能体...正在调用中");
        log.info("============");

        A2aAgent agent = A2aAgent.builder()
                .name("RouteMakingAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();

        log.info("============");
        log.info("获取到的远程Agent描述："+agent.getDescription());
        log.info("============");


        log.info("============");
        log.info("这个工具方法传入的参数："+ prompt);
        log.info("============");

        prompt = "制定最优驾车路线："+prompt+", 并预估费用";

        //组装Prompt
        PromptUtils promptUtils =  new PromptUtils();
        Msg userMsg = promptUtils.getPrompt(prompt);

        //远程Agent运行
        Msg remoteAgentResponse = null;
        try {

            log.info("============");
            log.info("远程 "+ agent.getName()+" 开始执行任务....");
            log.info("============");


            //阻塞运行
            remoteAgentResponse = agent.call(userMsg).block();
            System.out.println(remoteAgentResponse.getContent());
            String response = remoteAgentResponse.getTextContent();

            log.info("======= 远程Agent返回 ========");
            log.info(response);
            log.info("======================");

            //判断任务是否完成
            GenerateReason reason = remoteAgentResponse.getGenerateReason();

            log.info("============================");
            switch (reason) {
                case MODEL_STOP:
                    // 任务正常完成
                    log.info("此轮任务正常完成");
                    break;
                case INTERRUPTED:
                    // 任务被中断
                    log.info("此轮任务被中断");
                    break;
            }

            log.info("============================");

            return response;

        }catch (Exception e) {
            log.error("===================");
            log.error("Agent执行任务出错了！！");
            log.error("错误："+e.getMessage());
            log.error("===================");

            return "Agent执行出错了";
        }


    }

    /**
     * author: Imooc
     * description: 基于A2A协议获取行程规划Agent
     * @param :
     * @return void
     */
    @Tool(description = "擅长制定旅游景点,饮食,住宿的行程安排")
    public String callTripPlannerAgent(
            //工具参数
            @ToolParam(name = "prompt", description = "旅游目的地")
            String prompt) throws NacosException {

        log.info("============");
        log.info("工具方法：行程规划智能体...正在调用中");
        log.info("============");


        A2aAgent agent = A2aAgent.builder()
                .name("TripPlannerAgent")
                .agentCardResolver(
                        //创建 Nacos 的 AgentCardResolver
                        new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                .build();


        log.info("============");
        log.info("获取到的远程Agent描述："+agent.getDescription());
        log.info("============");


        log.info("============");
        log.info("这个工具方法传入的参数："+ prompt);
        log.info("============");


        prompt = "制定精彩旅游行程："+prompt+", 并预估费用";

        //组装Prompt
        PromptUtils promptUtils =  new PromptUtils();
        Msg userMsg = promptUtils.getPrompt(prompt);

        //远程Agent运行
        Msg remoteAgentResponse = null;
        try {

            log.info("============");
            log.info("远程 "+ agent.getName()+" 开始执行任务....");
            log.info("============");


            //阻塞运行
            remoteAgentResponse = agent.call(userMsg).block();
            System.out.println(remoteAgentResponse.getContent());
            String response = remoteAgentResponse.getTextContent();

            log.info("======= 远程Agent返回 ========");
            log.info(response);
            log.info("======================");

            //判断任务是否完成
            GenerateReason reason = remoteAgentResponse.getGenerateReason();

            log.info("============================");
            switch (reason) {
                case MODEL_STOP:
                    // 任务正常完成
                    log.info("此轮任务正常完成");
                    break;
                case INTERRUPTED:
                    // 任务被中断
                    log.info("此轮任务被中断");
                    break;
            }

            log.info("============================");

            return response;

        }catch (Exception e) {
            log.error("===================");
            log.error("Agent执行任务出错了！！");
            log.error("错误："+e.getMessage());
            log.error("===================");

            return "Agent执行出错了";
        }

    }


}