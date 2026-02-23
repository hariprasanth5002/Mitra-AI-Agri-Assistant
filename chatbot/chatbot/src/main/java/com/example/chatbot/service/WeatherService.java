package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class WeatherService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TextAIService textAIService;

    public String getTodayWeather(String city, String crop) {
        String promptForAI;
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    "http://localhost:8080/weather-advisory?city=" + city, Map.class
            );

            if (response != null) {
                String temperature = response.getOrDefault("temperature", "N/A").toString();
                String humidity = response.getOrDefault("humidity", "N/A").toString();
                String condition = response.getOrDefault("condition", "Unknown").toString();
                String cropAdvice = response.getOrDefault("cropAdvice", "No specific advice available.").toString();

                // --- FIX: Replaced the rigid prompt with a simpler, more conversational one ---
                promptForAI = String.format(
                    "You are Mitra, a friendly farm advisor. Give a short, conversational weather update for a farmer growing %s in %s. " +
                    "Use this data: Temperature is %s°C, humidity is %s%%, and conditions are '%s'. " +
                    "Incorporate this specific advice: '%s'. " +
                    "Combine everything into one smooth, helpful paragraph.",
                    crop, city, temperature, humidity, condition, cropAdvice
                );
                
            } else {
                promptForAI = "Weather data is not available for " + city;
            }
        } catch (Exception e) {
            e.printStackTrace();
            promptForAI = "Sorry, I could not retrieve the weather data for " + city + " right now.";
        }
        
        System.out.println("Sending this concise prompt to Text AI: " + promptForAI);
        return textAIService.getAdvisory(promptForAI);
    }
}