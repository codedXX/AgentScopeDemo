package com.dyx.hook;

import io.agentscope.core.agent.user.UserAgent;
import io.agentscope.core.hook.*;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * 计划拦截器
 */
@Slf4j
public class planHook implements Hook {

    //监听用户输入
    private final UserAgent user;
    //计划步骤
    private final PlanNotebook plan;

    //第n轮思考
    private int thinkingNum=1;

    public planHook(PlanNotebook planNotebook) {
        this.user = UserAgent.builder()
                .name("User")
                .build();
        this.plan=planNotebook;
    }

    /* **********************
     *
     * Hook 是对 HookEvent事件 的拦截
     * HookEvent事件：
     *
     * PreReasoningEvent：用户的输入事件
     * PostReasoningEvent: Agent推理思考过程事件
     * PreActingEvent： Agent执行过程准备调用工具的事件
     * PostActingEvent：Agent执行过程调用工具完成的事件
     *
     *
     * *********************/

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        /* **********************
         *
         * Hook 是对 HookEvent事件 的拦截
         * HookEvent事件：
         *
         * PreReasoningEvent：用户的输入事件
         * PostReasoningEvent: Agent推理思考过程事件
         * PreActingEvent： Agent执行过程准备调用工具的事件
         * PostActingEvent：Agent执行过程调用工具完成的事件
         *
         *
         * *********************/

        //匹配不同的事件
        switch (event) {

            //用户输入事件
            case PreReasoningEvent e -> {

//                String prompt = e.getInputMessages().get(0).getTextContent();
//                if(prompt != null) {
//                    log.info("========= 用户的Prompt：===========");
//                    log.info(prompt);
//                    log.info("=============================");
//                }

            }

            //推理思考事件
            case PostReasoningEvent e -> {

                String reason = e.getReasoningMessage().getTextContent();
                String agent = e.getReasoningMessage().getName();
                if(reason != null) {
                    log.info("=============="+agent+" 第 "+thinkingNum+" 轮思考：==================");
                    log.info(reason);
                    log.info("=============================");
                }

                thinkingNum++;

                //当计划列表已生成
//                Plan currentPlan = plan.getCurrentPlan();
//                if (currentPlan != null) {
//                    System.out.println("请输入修改意见: ");
//                    user.call().block();
//                }

            }


            //调用工具事件
            case PreActingEvent e -> {

                String toolName = e.getToolUse().getName();
                String agent = e.getAgent().getName();
                log.info("============"+agent+" 准备调用工具："+toolName+"=============");

            }

            //工具调用结果事件
            case PostActingEvent e -> {

                String res = e.getToolResultMsg().getTextContent();
                String tool = e.getToolUse().getName();
                String agent = e.getAgent().getName();

                //打印计划状态
                printPlanState(plan, "调用此工具：" +tool+"  的计划步骤");

                log.info("============"+agent+" 调用工具 "+tool+" 结果：=============");
                if(res !=null) {
                    log.info(res);
                }
                log.info("=========================");
            }


            default -> {
                // 其他事件忽略
            }
        }

        // 返回原事件
        return Mono.just(event);
    }


    /**
     * 打印计划执行状态
     */
    private static void printPlanState(PlanNotebook notebook, String event) {
        Plan currentPlan = notebook.getCurrentPlan();
        if (currentPlan == null) {
            log.info(" [" + event + "] 没有需要执行的计划");
            return;
        }

        log.info("======= 已生成本轮思考后的执行计划: "+event+"  ==========");
        log.info("计划名称: " + currentPlan.getName());
        log.info("执行状态: " + currentPlan.getState());
        log.info("====子任务:============");

        for (int i = 0; i < currentPlan.getSubtasks().size(); i++) {
            SubTask subtask = currentPlan.getSubtasks().get(i);
            String icon =
                    switch (subtask.getState()) {
                        case TODO -> "⏸  ";
                        case IN_PROGRESS -> "▶️";
                        case DONE -> "✅ ";
                        case ABANDONED -> "❌ ";
                    };
            System.out.printf(
                    "  %s [%d] %s - %s%n", icon, i, subtask.getName(), subtask.getState());
        }
        log.info("======================");
    }

}
