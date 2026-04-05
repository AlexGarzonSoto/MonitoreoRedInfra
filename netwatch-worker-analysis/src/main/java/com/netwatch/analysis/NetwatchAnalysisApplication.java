package com.netwatch.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NetwatchAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetwatchAnalysisApplication.class, args);
    }
}
