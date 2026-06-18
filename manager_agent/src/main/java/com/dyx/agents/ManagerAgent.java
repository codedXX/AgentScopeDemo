package com.dyx.agents;

import com.dyx.data.ResponseSchema;
import com.dyx.hook.planHook;
import com.dyx.plan.TripPlan;
import com.dyx.tool.RemoteAgentTool;
import com.dyx.utils.AgentUtils;
import com.dyx.utils.LangFuseUtils;
import com.dyx.utils.PromptUtils;
import com.dyx.utils.ToolUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.StructuredOutputReminder;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主管Agent
 */
@Component
@Slf4j
public class ManagerAgent {

    @Resource
    private AgentUtils agentUtils;

    @Resource
    private LangFuseUtils langFuseUtils;

    private ReActAgent agent;

    /**
     * author: Imooc
     * description: Agent 创建
     * @param :
     * @return null
     */
    public ReActAgent getManagerAgent() {


        //PlanNotebook
        TripPlan plan = new TripPlan();
        //Toolkit
        ToolUtils toolUtils = new ToolUtils();
        //将远程Agent封装为工具的封装注册到工具包
        Toolkit toolkit = toolUtils.getToolkit(new RemoteAgentTool());

        // 打印所有工具信息
        toolUtils.getTools();

        //计划对象
        PlanNotebook planNotebook = plan.getPlan();


        agent = agentUtils.getReActAgentBuilder(
                        "ManagerAgent",
                        "负责用户需求的解决方案和执行计划制定, 以及任务分发"
                )
                .sysPrompt("""
                    你是一个旅游管理主管。
                    当用户要求你规划旅游行程时，
                    请先创建一个详细的计划，
                    以及执行计划步骤,
                    并对每个计划步骤,要列出擅长执行这个步骤任务的Agent
                    然后按计划逐步执行。
                    """)
                /* **********************
                 *
                 * ReActAgent 能自主分解复杂任务, 并且会自动生成计划步骤：
                 * 1. .enablePlan()
                 *    1.1 但enablePlan方法不需要传递任何参数，也就是说无法对智能体的计划做自定义的设置
                 * 2. .planNotebook()
                 *
                 * .enablePlan() 内部调用了 PlanNotebook的Builder 构造方法
                 * 是采用默认的 PlanNotebook 的属性
                 *
                 * .planNotebook() 它是传入 PlanNotebook的 实例,
                 * 可以对 PlanNotebook 进行自定义
                 *
                 *
                 * PlanNotebook对象 是Agent能自主分解任务和步骤执行的核心
                 *
                 * PlanNotebook整个流程：
                 * 1. 复杂任务分解
                 * 2. 生成执行步骤
                 * 3. 状态跟踪
                 * 4. 动态调整
                 * 5. 任务完成
                 *
                 * PlanNotebook对象：自主规划 (PlanAct) + 自主决策 (ReAct)
                 *
                 *
                 *
                 *
                 * *********************/

                //自定义配置执行计划
                .planNotebook(planNotebook)
                //拦截器
                .hook(new planHook(planNotebook))
                //工具包
                .toolkit(toolkit)
                //结构化输出 (TOOL_CHOICE: 强制走工具调用产出结构化JSON, 适配qwen等支持工具调用的模型, 比PROMPT稳定)
                .structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)
                .build();


        return agent;


    }

    /**
     * author: Imooc
     * description: Agent 运行
     * @param :
     * @return void
     */
    public ResponseSchema run(String prompt) {
//        String prompt = """
//        帮我制定2026年元旦，
//        深圳到惠州3日游自驾游计划，
//        请包含吃住行，天气，酒店，餐饮美食。
//
//        你可以调用以下Agent处理子任务：
//        - routeMaking Agent: 擅长处理自驾游路线制定
//        - tripPlanner Agent: 擅长处理景点行程规划
//
//        - 每个子任务要注明调用的Agent
//        """;

        //构建Prompt
        PromptUtils promptUtils = new PromptUtils();

        //阻塞调用, 跑完整个 ReAct + 计划(planNotebook)流程, 拿到最终回复消息
        Msg reply = agent
                .call(List.of(promptUtils.getPrompt(prompt)))
                .block();

        //ResponseSchema 只有一个 response 字符串字段, 直接取最终回复文本即可。
        //(本版本 SDK 下 planNotebook 计划流程不会产出 _structured_output,
        // 故不走 getStructuredData, 直接用最终文本, 结果等价)
        ResponseSchema result = new ResponseSchema();
        result.response = (reply == null) ? "Agent未返回任何结果" : reply.getTextContent();
        return result;

    }

}