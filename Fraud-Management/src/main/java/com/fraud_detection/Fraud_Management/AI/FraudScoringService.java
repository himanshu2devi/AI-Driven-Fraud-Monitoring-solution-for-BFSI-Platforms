package com.fraud_detection.Fraud_Management.AI;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.graph.GraphRiskService;
import com.fraud_detection.Fraud_Management.ruleengine.ResultHolder;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FraudScoringService {

    @Autowired
    private AIFraudClient aiClient;

    @Autowired
    private FraudFeatureExtractor extractor;

    @Autowired
    private GraphRiskService graphRiskService;

    public void enrichWithAiScore(
            ResultHolder holder,
            TransactionDTO tx
    ) {

        double ruleScore =
                holder.getRuleScore();

        // =========================================
        //  FEATURE EXTRACTION
        // =========================================
        List<Double> features =
                extractor.extract(tx);

        // =========================================
        //  CALL PYTHON AI SERVICE
        // =========================================
        Map<String, Object> response =
                aiClient.getFraudScore(features);

        if (
                response == null
                        ||
                        response.get("fraud_score") == null
        ) {

            throw new RuntimeException(
                    "Invalid AI response: "
                            + response
            );
        }

        // =========================================
        //  AI SCORES
        // =========================================
        double aiScore =
                Double.parseDouble(
                        response.get("fraud_score")
                                .toString()
                );

        double mlScore =
                Double.parseDouble(
                        response.get("ml_score")
                                .toString()
                );

        double anomalyScore =
                Double.parseDouble(
                        response.get("anomaly_score")
                                .toString()
                );

        // =========================================
        //  GRAPH RISK SCORE
        // =========================================
        double graphRisk =
                graphRiskService
                        .calculateGraphRisk(
                                tx.getAccNoFrom()
                        );

        // =========================================
        //  DEBUG LOGS
        // =========================================
        System.out.println(
                "\n========== FRAUD ANALYSIS =========="
        );

        System.out.println(
                "Features: " + features
        );

        System.out.printf(
                "ML Score: %.4f%n",
                mlScore
        );

        System.out.printf(
                "Anomaly Score: %.4f%n",
                anomalyScore
        );

        System.out.printf(
                "AI Score: %.4f%n",
                aiScore
        );

        System.out.printf(
                "Rule Score: %.4f%n",
                ruleScore
        );

        System.out.printf(
                "Graph Risk Score: %.4f%n",
                graphRisk
        );

        StringBuilder finalReason =
                new StringBuilder();

        // =========================================
        //  EXISTING RULE REASONS
        // =========================================
        if (
                holder.getReason() != null
                        &&
                        !holder.getReason().isEmpty()
        ) {

            finalReason.append(
                    holder.getReason()
            );

            if (
                    !holder.getReason()
                            .endsWith(";")
            ) {

                finalReason.append("; ");
            }
        }

        String existingReason =
                holder.getReason() != null
                        ? holder.getReason()
                        : "";

        // =========================================
        //  AI EXPLAINABILITY
        // =========================================
        if (mlScore > 0.8) {

            finalReason.append(
                    "Strong fraud pattern detected by ML model; "
            );
        }

        if (anomalyScore > 0.7) {

            finalReason.append(
                    "Unusual transaction behavior detected; "
            );
        }

        // =========================================
        //  GRAPH EXPLAINABILITY
        // =========================================
        if (graphRisk >= 0.7) {

            finalReason.append(
                    "Account linked to historical fraud network; "
            );
        }

        // =========================================
        //  CRITICAL RULE OVERRIDES
        // =========================================

        // Blacklisted account
        if (
                existingReason.contains(
                        "Blacklisted account"
                )
        ) {

            holder.setAiScore(1.0);

            holder.setFinalScore(1.0);

            holder.setRiskLevel("HIGH");

            holder.setStatus(
                    TransactionStatus.FRAUD
            );

            finalReason.append(
                    "Critical blacklisted account transaction detected; "
            );

            holder.setReason(
                    formatReasons(
                            finalReason.toString()
                    )
            );

            System.out.println(
                    "Final Score: 1.0"
            );

            System.out.println(
                    "====================================\n"
            );

            return;
        }

        // Extreme transaction amount
        if (
                tx.getAmount() != null
                        &&
                        tx.getAmount() >= 10000000
        ) {

            holder.setAiScore(1.0);

            holder.setFinalScore(1.0);

            holder.setRiskLevel("HIGH");

            holder.setStatus(
                    TransactionStatus.FRAUD
            );

            finalReason.append(
                    "Extremely high-value transaction detected; "
            );

            holder.setReason(
                    formatReasons(
                            finalReason.toString()
                    )
            );

            System.out.println(
                    "Final Score: 1.0"
            );

            System.out.println(
                    "====================================\n"
            );

            return;
        }

        // =========================================
        //  CRITICAL AI OVERRIDE
        // =========================================
        if (

                aiScore >= 0.9

                        ||

                        (
                                mlScore >= 0.95
                                        &&
                                        graphRisk >= 0.6
                        )
        ) {

            holder.setAiScore(aiScore);

            holder.setFinalScore(1.0);

            holder.setRiskLevel("HIGH");

            holder.setStatus(
                    TransactionStatus.FRAUD
            );

            finalReason.append(
                    "Critical fraud confidence from hybrid AI engine; "
            );

            holder.setReason(
                    formatReasons(
                            finalReason.toString()
                    )
            );

            System.out.println(
                    "Final Score: 1.0"
            );

            System.out.println(
                    "====================================\n"
            );

            return;
        }

        // =========================================
        //  FINAL HYBRID SCORE
        // =========================================
        double finalScore =

                (0.45 * aiScore)

                        + (0.25 * ruleScore)

                        + (0.20 * graphRisk)

                        + (0.10 * anomalyScore);

        // =========================================
        //  ML SUSPICIOUS OVERRIDE
        // =========================================
        if (
                mlScore > 0.7
                        &&
                        finalScore < 0.4
        ) {

            finalScore = 0.45;
        }

        holder.setAiScore(aiScore);

        holder.setFinalScore(finalScore);

        System.out.printf(
                "Final Hybrid Score: %.4f%n",
                finalScore
        );

        // =========================================
        //  FINAL DECISION
        // =========================================
        if (finalScore >= 0.70) {

            holder.setRiskLevel("HIGH");

            holder.setStatus(
                    TransactionStatus.FRAUD
            );

            finalReason.append(
                    "High combined fraud score; "
            );
        }

        else if (finalScore >= 0.30) {

            holder.setRiskLevel("MEDIUM");

            holder.setStatus(
                    TransactionStatus.ALERT
            );

            finalReason.append(
                    "Moderate fraud risk; "
            );
        }

        else {

            holder.setRiskLevel("LOW");

            holder.setStatus(
                    TransactionStatus.VALID
            );

            finalReason.append(
                    "Low fraud risk; "
            );
        }

        // =========================================
        //  FINAL REASON FORMAT
        // =========================================
        String reasonText =
                finalReason.toString();

        if (
                reasonText.trim().isEmpty()
        ) {

            reasonText =
                    "AI-based fraud detection triggered";
        }

        holder.setReason(
                formatReasons(reasonText)
        );

        System.out.println(
                "====================================\n"
        );
    }

    // =========================================
    //  FORMAT REASONS
    // =========================================
    private String formatReasons(
            String reasonText
    ) {

        if (
                reasonText == null
                        ||
                        reasonText.isEmpty()
        ) {

            return "No significant risk detected";
        }

        String[] parts =
                reasonText.split(";");

        StringBuilder formatted =
                new StringBuilder();

        for (String part : parts) {

            if (!part.trim().isEmpty()) {

                formatted.append("\n- ")
                        .append(part.trim());
            }
        }

        return formatted.toString();
    }
}