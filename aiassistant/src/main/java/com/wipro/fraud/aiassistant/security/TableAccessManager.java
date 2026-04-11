package com.wipro.fraud.aiassistant.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TableAccessManager {

    // 🔥 Map intent → table
    public String getTableFromIntent(String intent) {

        return switch (intent) {
            case "TRANSACTIONS" -> "transactions";
            case "BLOCKED_ACCOUNT" -> "blocked_accounts";
            case "ACCOUNT_BALANCE" -> "account";
            case "FRAUD_CHECK" -> "fraud_alerts";
            default -> "unknown";
        };
    }

    // 🔥 Detect table from raw question (fallback)
    public String detectTableFromQuery(String query) {

        query = query.toLowerCase();

        if (query.contains("transaction")) return "transactions";
        if (query.contains("blocked")) return "blocked_accounts";
        if (query.contains("account")) return "account";
        if (query.contains("fraud")) return "fraud_alerts";
        if (query.contains("user")) return "users";

        return "unknown";
    }

    // 🔥 Role → Table access
    public boolean hasAccess(Set<String> roles, String table) {

        if (roles.contains("ROLE_ADMIN")) return true;

        return switch (table) {

            case "transactions",
                    "blocked_accounts",
                    "account",
                    "fraud_alerts",
                    "suspicious_merchants",
                    "knowledge_base",
                    "conversation_memory" ->
                    roles.contains("ROLE_FRAUDANALYST");

            default -> false;
        };
    }
}