package com.dyx.agents;

import com.dyx.utils.AgentUtils;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.JarSkillRepositoryAdapter;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.coding.ShellCommandTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * 景点推荐Agent
 */
@Component
@Slf4j
public class SuggestSightAgent {
    @Resource
    private AgentUtils agentUtils;

    //创建景点推荐Agent
    public ReActAgent getSuggestSightAgent() {
        Toolkit toolkit = new Toolkit();
        //构建skill，并将工具包和skill结合
        SkillBox skillBox = new SkillBox(toolkit);

        //====== 如果要用Python, 系统要安装好Python,以及放在在环境变量里 ==== //

        // 启用所有代码执行工具(Shell、读文件、写文件)
        skillBox.codeExecution()
                // 指定工作目录
                .workDir("./skill_file")
//                // 使用shell 工具
                .withShell(
                        new ShellCommandTool(
                                Set.of("bash")
                        ))
//                 启用文件读取
                .withRead()
                // 启用文件写入
                .withWrite()
                .enable();


        //以文件形式读取Skill.md
        JarSkillRepositoryAdapter repo = null;
        try {
            repo = new JarSkillRepositoryAdapter(
                    "skills"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //景点住宿推荐技能
        AgentSkill SuggestSightsSkill = repo.getSkill("Suggest-Sights");
        //表格制作技能
        AgentSkill tableSkill = repo.getSkill("Make-Table");
        skillBox.registerSkill(SuggestSightsSkill);

        log.info("========== 所有 Skills：===========");
        skillBox.getAllSkillIds().stream().forEach(item -> log.info(item));
        log.info("==============================");

        return agentUtils.getReActAgentBuilder(
                        "SuggestSightAgent",
                        "专注于景点推荐的 SubAgent"
                )
                //挂载工具包
                .toolkit(toolkit)
                //挂载Skills
                .skillBox(skillBox)
                .build()
                ;

    }
}
