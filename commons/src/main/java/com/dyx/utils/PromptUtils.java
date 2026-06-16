package com.dyx.utils;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * description: Prompt构建工具类
 */
@Slf4j
public class PromptUtils {

    public Msg getPrompt(String prompt)
    {

        log.info("====== 构建的Prompt ======");
        log.info(prompt);
        log.info("============");


        //Prompt
       return Msg.builder()
                //消息角色
                .role(MsgRole.USER)
                //消息内容 (Prompt)
                .content(List.of(
                        TextBlock.builder()
                                .text(prompt)
                                .build()
                ))
                .build();


    }
}