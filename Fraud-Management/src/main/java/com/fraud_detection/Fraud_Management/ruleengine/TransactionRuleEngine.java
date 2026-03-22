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

    public TransactionResult evaluateTransaction(TransactionDTO transaction) {
        for (TransactionRule rule : rules) {
            TransactionResult result = rule.apply(transaction);
            if (result.getStatus() != TransactionStatus.VALID) {
                return result;
            }
        }
        return new TransactionResult(TransactionStatus.VALID, "All rules passed.");
    }
}