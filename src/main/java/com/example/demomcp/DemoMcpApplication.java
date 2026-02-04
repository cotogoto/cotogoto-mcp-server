package com.example.demomcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoMcpApplication.class, args);
    }

    @Bean
    ToolCallbackProvider toolCallbackProvider(WeatherService weatherService) {
        return ToolCallbacks.from(weatherService);
    }
}
