package com.example.demo.service;

import com.example.demo.model.WeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    @Value("${openweathermap.api.key}")
    private String apiKey;

    @Value("${openweathermap.api.url}")
    private String apiUrl;

    public WeatherResponse getTodayWeather(String city) {
        RestTemplate restTemplate = new RestTemplate();
        String url = apiUrl + "?q=" + city + "&appid=" + apiKey + "&units=metric";
        return restTemplate.getForObject(url, WeatherResponse.class);
    }
}
