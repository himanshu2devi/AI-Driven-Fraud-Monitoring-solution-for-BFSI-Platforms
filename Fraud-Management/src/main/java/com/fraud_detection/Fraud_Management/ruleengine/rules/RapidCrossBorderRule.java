package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RapidCrossBorderRule implements TransactionRule {

    private static final Set<String> crossBorderHistory = new HashSet<>();

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        if (!"INR".equalsIgnoreCase(txn.getCurrency())) {
            if (crossBorderHistory.contains(txn.getAccNoFrom())) {
                return new TransactionResult(TransactionStatus.FRAUD, "Rapid cross-border transactions.");
            }
            crossBorderHistory.add(txn.getAccNoFrom());
        }
        return new TransactionResult(TransactionStatus.VALID, "No cross-border issue.");
    }
}
