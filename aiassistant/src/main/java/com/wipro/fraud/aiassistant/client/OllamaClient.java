package com.wipro.fraud.aiassistant.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    public OllamaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateResponse(String prompt) {

        String url = baseUrl + "/api/generate";

        Map<String, Object> request = Map.of(
                "model", "phi4-mini",
                "prompt", prompt,
                "stream", false
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response =
                (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);

        // 🔥 Safety check
        if (response == null || response.get("response") == null) {
            throw new RuntimeException("Invalid response from Ollama");
        }

        return response.get("response").toString();
    }
}