package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Advisory {
    private double temperature;
    private double humidity;
    private String condition;
    private String cropAdvice;
}
