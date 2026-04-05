package com.netwatch.alerts;

import com.netwatch.alerts.config.AlertProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AlertProperties.class)
public class NetwatchAlertsApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetwatchAlertsApplication.class, args);
    }
}
