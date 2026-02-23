package com.example.demo.controller;

import com.example.demo.model.Advisory;
import com.example.demo.model.WeatherResponse;
import com.example.demo.service.AdvisoryService;
import com.example.demo.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather-advisory")
@CrossOrigin(origins = "*")
public class WeatherAdvisoryController {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private AdvisoryService advisoryService;

    @GetMapping
    public ResponseEntity<Advisory> getAdvisory(@RequestParam String city) {
        WeatherResponse todayWeather = weatherService.getTodayWeather(city);
        Advisory advisory = advisoryService.getAdvisory(todayWeather);
        return ResponseEntity.ok(advisory);
    }
}
