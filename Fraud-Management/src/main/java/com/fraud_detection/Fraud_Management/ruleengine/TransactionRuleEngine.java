package com.fraud_detection.Fraud_Management.ruleengine;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionRuleEngine {

    private final List<TransactionRule> rules;

    public TransactionRuleEngine(List<TransactionRule> rules) {
        this.rules = rules;
    }

    public ResultHolder evaluateTransaction(TransactionDTO transaction) {

        ResultHolder holder = new ResultHolder();

        double ruleScore = 0;
        StringBuilder reasons = new StringBuilder();

        for (TransactionRule rule : rules) {

            TransactionResult result = rule.apply(transaction);

            if (result.getStatus() == TransactionStatus.FRAUD) {
                ruleScore += 0.4;
                reasons.append(result.getReason()).append("; ");
            }

            else if (result.getStatus() == TransactionStatus.ALERT) {
                ruleScore += 0.2;
                reasons.append(result.getReason()).append("; ");
            }
        }

        holder.setReason(reasons.toString());
        holder.setStatus(TransactionStatus.VALID);
        holder.setRuleScore(Math.min(ruleScore, 1.0));

        return holder;
    }
}