package com.dyx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.dyx"})
public class ManagerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerAgentApplication.class, args);
    }

}
