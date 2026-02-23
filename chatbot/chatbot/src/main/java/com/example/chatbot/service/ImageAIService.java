package com.example.chatbot.service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ImageAIService {

    private final RestTemplate restTemplate;

    public ImageAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Not used (we removed URL flow per your request) - kept if needed in future
    public String predictDisease(String imageUrl) {
        Map<String, String> request = Map.of("image_url", imageUrl);
        Map response = restTemplate.postForObject("http://localhost:5002/predict", request, Map.class);
        System.out.println("Image AI response (url): " + response);
        String raw = extractRawPrediction(response);
        return normalizePrediction(raw);
    }

    // File-based predict: send multipart to image model at /predict
    public String predictDiseaseFromFile(MultipartFile file) throws Exception {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:5002")
                .build();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

        Map response = client.post()
                .uri("/predict")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        System.out.println("Image AI response (file): " + response);

        String raw = extractRawPrediction(response);
        String normalized = normalizePrediction(raw);
        return normalized;
    }

    private String extractRawPrediction(Map response) {
        if (response == null) return null;
        // try common keys returned by your image service
        if (response.get("predicted_disease") != null) return response.get("predicted_disease").toString();
        if (response.get("predicted_class") != null) return response.get("predicted_class").toString();
        if (response.get("predicted_label") != null) return response.get("predicted_label").toString();
        // sometimes the payload might be nested; handle simply
        return null;
    }

    // Normalize underscores -> readable title-cased string
    private String normalizePrediction(String raw) {
        if (raw == null) return null;
        // replace one-or-more underscores with space
        String s = raw.replaceAll("_+", " ").trim();
        // convert to Title Case (first letter uppercase, rest lowercase) for each word
        String[] parts = s.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.length() == 0) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
