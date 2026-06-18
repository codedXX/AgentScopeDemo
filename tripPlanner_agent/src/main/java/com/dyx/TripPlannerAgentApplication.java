package com.dyx;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TripPlannerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripPlannerAgentApplication.class, args);
    }

}
