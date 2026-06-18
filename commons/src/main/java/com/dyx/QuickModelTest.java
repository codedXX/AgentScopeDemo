package com.dyx;

import com.dyx.utils.AgentUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

import java.util.List;

public class QuickModelTest {
    public static void main(String[] args) {
        // 临时把你的真实 Key 填这里（验证完连同本文件一起删除，别提交 Git）
        String apiKey = "sk-255506ca196b48f38e686b3e82efac58";
        // 先用通用名验证连通；通了再换成 .env 里的 qwen3.7-max 复测一次
        String modelName = "qwen3.7-max";

        AgentUtils agentUtils = new AgentUtils();
        ReActAgent reActAgentBuilder = agentUtils.getReActAgentBuilder("quickTest", "测试一下");
        agentUtils.streamResponse(reActAgentBuilder, "用一句话介绍你自己");
    }
}