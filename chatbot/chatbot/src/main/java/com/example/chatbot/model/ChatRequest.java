package com.example.chatbot.model;

public class ChatRequest {
    private String text;
    private String imageUrl;
    private String voiceUrl;
    private String crop;
    private String city;

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String voiceUrl) { this.voiceUrl = voiceUrl; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
