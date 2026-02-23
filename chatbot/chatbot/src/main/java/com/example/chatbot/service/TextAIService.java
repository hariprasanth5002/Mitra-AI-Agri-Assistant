package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class TextAIService {

    @Autowired
    private RestTemplate restTemplate;

    public String getAdvisory(String text) {
        if (text == null || text.isBlank()) return "No advisory found.";

        try {
            Map<String, String> request = Map.of("text", text);
            Map response = restTemplate.postForObject("http://localhost:5004/generate", request, Map.class);
            System.out.println("Text AI raw response: " + response);

            if (response == null) return "No advisory found.";

            String agriqAdvisory = response.get("agriq_advisory") != null ? response.get("agriq_advisory").toString().trim() : "";
            String ruleBased = response.get("rule_based") != null ? response.get("rule_based").toString().trim() : "";

            // If agriq is only echo of input or just "Input :" (useless), treat as empty
            if (isUselessEcho(agriqAdvisory, text)) agriqAdvisory = "";

            StringBuilder combined = new StringBuilder();

            // AI (agriq) first if present
            if (!agriqAdvisory.isBlank()) {
                combined.append(agriqAdvisory);
            }

            // then rule-based
            if (!ruleBased.isBlank()) {
                if (combined.length() > 0) combined.append(" ");
                combined.append(ruleBased);
            }

            // AgroBERT masked predictions (optional, appended as possible terms)
            if (response.get("masked_predictions") instanceof List) {
                List<Map<String, Object>> maskedList = (List<Map<String, Object>>) response.get("masked_predictions");
                if (!maskedList.isEmpty()) {
                    if (combined.length() > 0) combined.append(" ");
                    combined.append("Possible terms: ");
                    for (Map<String, Object> pred : maskedList) {
                        combined.append(pred.get("token_str")).append(", ");
                    }
                    // remove final comma
                    combined.setLength(combined.length() - 2);
                }
            }

            String out = combined.toString().trim();
            return out.isEmpty() ? "No advisory found for: " + text : out;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling Text AI: " + e.getMessage();
        }
    }

    private boolean isUselessEcho(String agriq, String original) {
        if (agriq == null || agriq.isBlank()) return true;
        String cleaned = agriq.replace(":", "").trim();
        if (cleaned.equalsIgnoreCase(original.trim())) return true;
        return false;
    }
}
