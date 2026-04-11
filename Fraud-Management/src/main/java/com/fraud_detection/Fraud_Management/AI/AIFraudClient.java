package com.fraud_detection.Fraud_Management.AI;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class AIFraudClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public double getFraudScore(List<Double> features) {

        String url = "http://localhost:8000/predict";

        Map<String, Object> req = new HashMap<>();
        req.put("features", features);

        Map response = restTemplate.postForObject(url, req, Map.class);

        return Double.parseDouble(response.get("fraud_score").toString());
    }
}
