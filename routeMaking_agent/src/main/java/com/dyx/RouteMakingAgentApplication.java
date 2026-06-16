package com.dyx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.dyx"})
public class RouteMakingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteMakingAgentApplication.class, args);
    }

}
