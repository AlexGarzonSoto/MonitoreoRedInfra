package com.netwatch.osint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NetwatchOsintApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetwatchOsintApplication.class, args);
    }
}
