package com.fraud_detection.Fraud_Management.AI;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.ResultHolder;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudScoringService {

    @Autowired
    private AIFraudClient aiClient;

    @Autowired
    private FraudFeatureExtractor extractor;

    public void enrichWithAiScore(ResultHolder holder, TransactionDTO tx) {

        double ruleScore = holder.getRuleScore();

        List<Double> features = extractor.extract(tx);

        double aiScore = aiClient.getFraudScore(features);

        StringBuilder finalReason = new StringBuilder();

        // 🔹 Include existing rule-based reasons (if any)
        if (holder.getReason() != null && !holder.getReason().isEmpty()) {
            finalReason.append(holder.getReason());
        }

        //  AI OVERRIDE (CRITICAL FRAUD)
        if (aiScore > 0.9) {

            holder.setAiScore(aiScore);
            holder.setFinalScore(aiScore);
            holder.setRiskLevel("HIGH");
            holder.setStatus(TransactionStatus.FRAUD);

            //  Add AI reason



            // include rule reason if exists
            if (holder.getReason() != null && !holder.getReason().isEmpty()) {
                finalReason.append(holder.getReason());
            }


            finalReason.append("High AI fraud probability; ");

            holder.setReason(formatReasons(finalReason.toString()));
            return;
        }

        double finalScore = 0.7 * aiScore + 0.3 * ruleScore;

        holder.setAiScore(aiScore);
        holder.setFinalScore(finalScore);

        System.out.println("Rule Score: " + ruleScore);
        System.out.println("AI Score: " + aiScore);
        System.out.println("Final Score: " + finalScore);

        //  FINAL DECISION + REASONS
        if (finalScore > 0.8) {
            holder.setRiskLevel("HIGH");
            holder.setStatus(TransactionStatus.FRAUD);
            finalReason.append("High combined fraud score; ");
        }
        else if (finalScore > 0.5) {
            holder.setRiskLevel("MEDIUM");
            holder.setStatus(TransactionStatus.ALERT);
            finalReason.append("Moderate fraud risk; ");
        }
        else {
            holder.setRiskLevel("LOW");
            holder.setStatus(TransactionStatus.VALID);
            finalReason.append("Low fraud risk; ");
        }

        //  FINAL FORMATTED REASON
        String reasonText = finalReason.toString();


        if (reasonText == null || reasonText.trim().isEmpty()) {
            reasonText = "AI-based fraud detection triggered";
        }

        holder.setReason(formatReasons(reasonText));
    }

    //  FORMAT INTO BULLET POINTS
    private String formatReasons(String reasonText) {

        if (reasonText == null || reasonText.isEmpty()) {
            return "No significant risk detected";
        }

        String[] parts = reasonText.split(";");

        StringBuilder formatted = new StringBuilder();

        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                formatted.append("\n- ").append(part.trim());
            }
        }

        return formatted.toString();
    }
}