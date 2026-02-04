package com.example.demomcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    @Tool(description = "Get the current weather summary for a city.")
    public String getWeather(String city) {
        return "Weather for " + city + ": sunny (sample response).";
    }
}
