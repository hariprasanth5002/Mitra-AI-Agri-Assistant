package com.example.demo.model;

import lombok.Data;

@Data
public class WeatherResponse {
    private Main main;
    private Weather[] weather;

    @Data
    public static class Main {
        private double temp;
        private double humidity;
    }

    @Data
    public static class Weather {
        private String main;
        private String description;
    }
}
