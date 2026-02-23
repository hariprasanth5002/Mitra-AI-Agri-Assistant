// src/main/java/com/example/chatbot/service/VoiceAIService.java
package com.example.chatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class VoiceAIService {

    @Autowired
    private RestTemplate restTemplate;

    public String transcribeVoice(MultipartFile voiceFile) {
        if (voiceFile == null || voiceFile.isEmpty()) {
            return "No audio file provided.";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", voiceFile.getResource());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = "http://localhost:5001/transcribe";

            // The Python service returns a map with "final_transcription"
            Map<String, Object> response = restTemplate.postForObject(url, requestEntity, Map.class);
            
            if (response != null && response.get("final_transcription") != null) {
                return response.get("final_transcription").toString();
            }
            return "Could not understand the audio.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during voice transcription.";
        }
    }
}