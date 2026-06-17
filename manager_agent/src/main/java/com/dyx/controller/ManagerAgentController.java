package com.dyx.controller;

import com.dyx.agents.ManagerAgent;
import com.dyx.data.PromptSchema;
import com.dyx.data.ResponseSchema;
import io.agentscope.core.ReActAgent;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagerAgentController {

    @Resource
    private ManagerAgent managerAgent;

    // 用户提交旅游规划的Prompt
    @RequestMapping(
            value = "/trip",
            produces = "application/json;charset=UTF-8",
            method = RequestMethod.POST)
    public ResponseSchema tripPlan(@RequestBody PromptSchema input) {

        ReActAgent manager = managerAgent.getManagerAgent();
        ResponseSchema response = managerAgent.run(input.getPrompt());

        return response;
    }
}