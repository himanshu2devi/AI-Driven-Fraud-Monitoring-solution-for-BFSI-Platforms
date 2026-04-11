package com.wipro.fraud.aiassistant.security;


import org.springframework.stereotype.Component;

@Component
public class PromptSecurityFilter {

    public boolean isMalicious(String input) {

        String lower = input.toLowerCase();

        return lower.contains("ignore previous instructions") ||
                lower.contains("bypass") ||
                lower.contains("reveal system prompt") ||
                lower.contains("show database") ||
                lower.contains("password") ||
                lower.contains("token") ||
                lower.contains("api key") ||
                lower.contains("confidential");
    }

    public boolean isUnsafeResponse(String response) {

        String lower = response.toLowerCase();

        return lower.contains("password") ||
                lower.contains("api key") ||
                lower.contains("internal system") ||
                lower.contains("database schema");
    }
}
