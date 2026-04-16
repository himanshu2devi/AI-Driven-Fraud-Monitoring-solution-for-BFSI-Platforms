package com.wipro.fraud.aiassistant.Agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FraudInvestigationAgent {

    private final FraudReadOnlyAgent agent;

    public FraudInvestigationAgent(FraudReadOnlyAgent agent) {
        this.agent = agent;
    }

    public void investigate(String accountNumber) {

        System.out.println("\n🕵️ Investigation Started for Account: " + accountNumber);

        try {

            // ===============================
            // STEP 1: GET DATA + PRINT
            // ===============================
            String txQuery = "show transactions of account " + accountNumber;

            List<Map<String, Object>> txData = agent.getData(txQuery); // 🔥 GET DATA
            agent.execute(txQuery); // print

            // ===============================
            // STEP 2: DECISION (REAL LOGIC)
            // ===============================
            String decision = decideNextStep(txData);

            // ===============================
            // FRAUD / ALERT (HIGHEST PRIORITY)
            // ===============================
            if (decision.equals("check fraud")) {

                runStep("🚨 Checking fraud/alert transactions",
                        "show fraud transactions of account " + accountNumber);

                runStep("🧠 AI Fraud Analysis",
                        "why transactions are risky for account " + accountNumber);

                System.out.println("\n🚨 High Risk Account Detected");
                return;
            }

            // ===============================
            // FAILED (SECOND PRIORITY)
            // ===============================
            if (decision.equals("check failed")) {

                runStep("❌ Checking failed transactions",
                        "show failed transactions of account " + accountNumber);

                String analysis = runStep("🧠 AI Failure Analysis",
                        "why transactions are failing for account " + accountNumber);

// 🔥 DYNAMIC RISK
                if (analysis != null) {

                    String lower = analysis.toLowerCase();

                    if (lower.contains("high") || lower.contains("fraud") || lower.contains("🚨")) {
                        System.out.println("\n🚨 High Risk Account Detected");
                    }
                    else if (lower.contains("medium") || lower.contains("alert")) {
                        System.out.println("\n⚠️ Medium Risk Account");
                    }
                    else {
                        System.out.println("\n🟢 Low Risk Account");
                    }
                }


                return;
            }

            // ===============================
            // SAFE
            // ===============================
            System.out.println("\n✅ No suspicious activity detected. Low risk account.");

        } catch (Exception e) {
            System.out.println("❌ Investigation error: " + e.getMessage());
        }
    }

    public void investigateNetwork(String accountNumber) {

        System.out.println("\n🕸️ Fraud Network Analysis for Account: " + accountNumber);

        try {

            // Step 1: Get transactions of main account
            List<Map<String, Object>> data =
                    agent.getData("show transactions of account " + accountNumber);

            if (data == null || data.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }

            boolean fraudChainFound = false;

            for (Map<String, Object> txn : data) {

                String toAcc = String.valueOf(txn.get("to_account"));

                if (toAcc == null || toAcc.isBlank()) continue;

                System.out.println("\n➡ Checking linked account: " + toAcc);

                // Step 2: Check linked account transactions
                List<Map<String, Object>> nextLevel =
                        agent.getData("show transactions of account " + toAcc);

                boolean fraudFound = nextLevel.stream().anyMatch(t -> {
                    String status = String.valueOf(t.get("status")).toUpperCase();
                    return status.contains("FRAUD") || status.contains("ALERT");
                });

                if (fraudFound) {
                    fraudChainFound = true;
                    System.out.println("🚨 Fraud chain detected via: " + toAcc);
                }
            }

            if (!fraudChainFound) {
                System.out.println("\n✅ No fraud chain detected");
            } else {
                System.out.println("\n🚨 Network Risk: HIGH");
            }

        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }

    // ===============================
    // 🔥 DECISION ENGINE (CORE LOGIC)
    // ===============================
    private String decideNextStep(List<Map<String, Object>> data) {

        if (data == null || data.isEmpty()) {
            return "safe";
        }

        boolean hasFraudOrAlert = data.stream().anyMatch(t -> {
            String status = String.valueOf(t.get("status")).toUpperCase();
            return status.equals("FRAUD") || status.equals("ALERT");
        });

        boolean hasFailed = data.stream().anyMatch(t ->
                "FAILED".equalsIgnoreCase(String.valueOf(t.get("status")))
        );

        // 🔥 PRIORITY ORDER
        if (hasFraudOrAlert) return "check fraud";
        if (hasFailed) return "check failed";

        return "safe";
    }

    // ===============================
    // STEP EXECUTION
    // ===============================
    private String runStep(String title, String query) throws InterruptedException {

        System.out.println("\n" + title);
        System.out.println("🧠 Thinking...");

        Thread.sleep(700);

        return agent.executeWithResponse(query);
    }
}