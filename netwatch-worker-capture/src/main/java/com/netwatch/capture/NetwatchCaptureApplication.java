package com.netwatch.capture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.netwatch.capture.config.CaptureProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(CaptureProperties.class)
public class NetwatchCaptureApplication {
    public static void main(String[] args) {
        SpringApplication.run(NetwatchCaptureApplication.class, args);
    }
}
