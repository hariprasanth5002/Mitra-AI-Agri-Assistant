// src/main/java/com/example/chatbot/service/MarketService.java
package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Map;

@Service
public class MarketService {

    @Autowired
    private RestTemplate restTemplate;

    public String getMarketInfo(String crop, String district, String market) {
        String state = "Punjab";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:5004/market-price-latest")
                .queryParam("state", state);

        if (crop != null && !crop.isBlank()) builder.queryParam("commodity", crop);
        if (district != null && !district.isBlank()) builder.queryParam("district", district);
        if (market != null && !market.isBlank()) builder.queryParam("market", market);

        URI uri = builder.build().toUri();
        
        try {
            Map<String, String> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.containsKey("market_info")) {
                return response.get("market_info");
            }
            return "I'm sorry, I couldn't parse the market information.";
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "I'm sorry, but I couldn't find any specific market data for your query.";
            }
            return "The market data service seems to be having a temporary problem.";
        } catch (Exception e) {
            return "I'm having a little trouble reaching the market data service right now.";
        }
    }
}