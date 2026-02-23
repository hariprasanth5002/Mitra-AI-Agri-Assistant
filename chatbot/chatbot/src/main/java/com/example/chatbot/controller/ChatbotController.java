// src/main/java/com/example/chatbot/controller/ChatbotController.java
package com.example.chatbot.controller;

import com.example.chatbot.model.ChatRequest;
import com.example.chatbot.model.ChatResponse;
import com.example.chatbot.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/chatbot")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired private ImageAIService imageAIService;
    @Autowired private VoiceAIService voiceAIService;
    @Autowired private TextAIService textAIService;
    @Autowired private WeatherService weatherService;
    @Autowired private MarketService marketService;

    @Value("${chatbot.default.crop}") private String DEFAULT_CROP;
    @Value("${chatbot.default.district}") private String DEFAULT_DISTRICT; // Changed from city

    // Updated lists to distinguish districts from markets
    private static final List<String> KNOWN_CROPS = Arrays.asList("wheat", "potato", "paddy", "tomato", "onion", "apple", "maize");
    private static final List<String> KNOWN_DISTRICTS = Arrays.asList("amritsar", "ludhiana", "jalandhar", "hoshiarpur","coimbatore");
    private static final List<String> KNOWN_MARKETS = Arrays.asList("rayya", "sahnewal", "tanda urmur", "khanna", "moga");

    // The JSON endpoint is now for text-only queries
    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatResponse> askJson(@RequestBody ChatRequest request) {
        String text = request.getText();
        String advisory = processTextRequest(text, request.getCrop(), request.getCity()); // City field can be used as district
        return ResponseEntity.ok(new ChatResponse(advisory));
    }

    // The Multipart endpoint is the main entry point for all types of requests
    @PostMapping(value = "/ask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponse> askMultipart(
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "voice", required = false) MultipartFile voiceFile,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "crop", required = false) String crop,
            @RequestParam(value = "city", required = false) String city) { // city field can be used as district
        try {
            String advisory = null;
            // Priority 1: Image file
            if (imageFile != null && !imageFile.isEmpty()) {
                String predicted = imageAIService.predictDiseaseFromFile(imageFile);
                advisory = textAIService.getAdvisory(predicted);
            } 
            // Priority 2: Voice file
            else if (voiceFile != null && !voiceFile.isEmpty()) {
                String transcribedText = voiceAIService.transcribeVoice(voiceFile);
                advisory = processTextRequest(transcribedText, crop, city);
            } 
            // Priority 3: Text input
            else if (text != null && !text.isBlank()) {
                advisory = processTextRequest(text, crop, city);
            }
            
            if (advisory == null || advisory.isBlank()) advisory = "Please provide an input (text, voice, or image).";
            return ResponseEntity.ok(new ChatResponse(advisory));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ChatResponse("An error occurred: " + e.getMessage()));
        }
    }

    // Central text processing logic used by both JSON and Multipart endpoints
    private String processTextRequest(String text, String crop, String district) {
        if (text == null || text.isBlank()) return "No text provided.";
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("weather")) {
            String districtFromText = findEntityInText(lowerText, KNOWN_DISTRICTS).orElse(district);
            String districtToUse = (districtFromText != null && !districtFromText.isBlank()) ? districtFromText : DEFAULT_DISTRICT;
            String cropToUse = (crop != null && !crop.isBlank()) ? crop : DEFAULT_CROP;
            return weatherService.getTodayWeather(districtToUse, cropToUse);
        } 
        else if (lowerText.contains("market") || lowerText.contains("price")) {
            String cropFromText = findEntityInText(lowerText, KNOWN_CROPS).orElse(crop);
            String districtFromText = findEntityInText(lowerText, KNOWN_DISTRICTS).orElse(district);
            String marketFromText = findEntityInText(lowerText, KNOWN_MARKETS).orElse(null);

            String cropToUse = (cropFromText != null && !cropFromText.isBlank()) ? cropFromText : DEFAULT_CROP;
            String districtToUse = (districtFromText != null && !districtFromText.isBlank()) ? districtFromText : DEFAULT_DISTRICT;
            
            return marketService.getMarketInfo(cropToUse, districtToUse, marketFromText);
        } 
        else {
            return textAIService.getAdvisory(text);
        }
    }

    private Optional<String> findEntityInText(String text, List<String> entities) {
        return entities.stream()
                .filter(entity -> text.contains(entity))
                .findFirst();
    }
}