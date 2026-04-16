package com.wipro.fraud.aiassistant.security;

import org.springframework.stereotype.Component;

@Component
public class PromptSecurityFilter {

    // =========================
    // 🔐 INPUT CHECK
    // =========================
    public SecurityResult checkInput(String input) {

        String q = input.toLowerCase();

        // 🚫 HIGH RISK (block immediately)
        if (q.contains("bypass")
                || q.contains("hack")
                || q.contains("launder")
                || q.contains("evade")
                || q.contains("exploit")) {

            return new SecurityResult(true,
                    "⚠️ This request violates security policies.",
                    SecurityLevel.BLOCK);
        }

        // ⚠️ PROMPT INJECTION
        if (q.contains("ignore previous instructions")
                || q.contains("reveal system prompt")
                || q.contains("show database")
                || q.contains("api key")
                || q.contains("token")
                || q.contains("password")) {

            return new SecurityResult(true,
                    "⚠️ Suspicious request detected. Cannot process this query.",
                    SecurityLevel.BLOCK);
        }

        // ⚠️ SOFT WARNING (allowed but monitored)
        if (q.contains("confidential")
                || q.contains("internal")) {

            return new SecurityResult(false,
                    "⚠️ Limited information will be shown due to policy.",
                    SecurityLevel.WARN);
        }

        return new SecurityResult(false, null, SecurityLevel.ALLOW);
    }


    // =========================
    // 🔐 RESPONSE CHECK
    // =========================
    public SecurityResult checkResponse(String response) {

        if (response == null) {
            return new SecurityResult(false, null, SecurityLevel.ALLOW);
        }

        String r = response.toLowerCase();

        // 🚫 LLM BLOCK MESSAGE (your current issue)
        if (r.contains("blocked by our content filters")) {
            return new SecurityResult(true,
                    "⚠️ AI response restricted. Showing system data instead.",
                    SecurityLevel.FALLBACK);
        }

        // 🚫 SENSITIVE DATA LEAK
        if (r.contains("password")
                || r.contains("api key")
                || r.contains("token")
                || r.contains("database schema")
                || r.contains("internal system")) {

            return new SecurityResult(true,
                    "⚠️ Sensitive information detected. Response blocked.",
                    SecurityLevel.BLOCK);
        }

        return new SecurityResult(false, null, SecurityLevel.ALLOW);
    }


    // =========================
    // 🔥 RESULT CLASS
    // =========================
    public static class SecurityResult {

        private boolean flagged;
        private String message;
        private SecurityLevel level;

        public SecurityResult(boolean flagged, String message, SecurityLevel level) {
            this.flagged = flagged;
            this.message = message;
            this.level = level;
        }

        public boolean isFlagged() { return flagged; }
        public String getMessage() { return message; }
        public SecurityLevel getLevel() { return level; }
    }

    // =========================
    // 🔥 LEVEL ENUM
    // =========================
    public enum SecurityLevel {
        ALLOW,
        WARN,
        BLOCK,
        FALLBACK
    }
}