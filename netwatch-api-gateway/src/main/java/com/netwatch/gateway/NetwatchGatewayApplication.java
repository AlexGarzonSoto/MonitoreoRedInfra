package com.netwatch.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NetwatchGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetwatchGatewayApplication.class, args);
    }
}