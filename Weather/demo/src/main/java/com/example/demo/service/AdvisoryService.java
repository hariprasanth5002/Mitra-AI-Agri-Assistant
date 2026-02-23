package com.example.demo.service;

import com.example.demo.model.Advisory;
import com.example.demo.model.WeatherResponse;
import org.springframework.stereotype.Service;

@Service
public class AdvisoryService {

    public Advisory getAdvisory(WeatherResponse todayWeather) {
        double temp = todayWeather.getMain().getTemp();
        double humidity = todayWeather.getMain().getHumidity();
        String condition = todayWeather.getWeather()[0].getMain();

        String advisoryMessage;

        if (condition.equalsIgnoreCase("Rain")) {
            advisoryMessage = "Rain expected today. Avoid pesticide spraying.";
        } else if (temp > 35) {
            advisoryMessage = "High temperature today. Irrigate fields early or late to prevent water stress.";
        } else if (humidity > 80) {
            advisoryMessage = "High humidity today. Watch out for fungal infections.";
        } else {
            advisoryMessage = "Weather is normal today. Continue regular farming operations.";
        }

        return new Advisory(temp, humidity, condition, advisoryMessage);
    }
}
