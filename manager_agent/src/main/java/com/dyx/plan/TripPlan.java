package com.dyx.plan;

import io.agentscope.core.plan.PlanNotebook;

/**
 * 自定义 Agent自主分解旅游规划任务
 */
public class TripPlan {
    /**
     * 自定义 PlanNotebook 实例
     */
    public PlanNotebook getPlan() {
        return PlanNotebook.builder()
                //计划步骤是否需要用户确认
                .needUserConfirm(false)
                //分解出来的子任务数量限制
                .maxSubtasks(5)
                //计划的存储方式
                //.storage()
                .build();
    }
}
