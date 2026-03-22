package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RepeatedTransactionRule implements TransactionRule {

    private static final Map<String, String> recentTransactions = new HashMap<>();

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        String key = txn.getAccNoFrom() + "-" + txn.getAccNoTo() + "-" + txn.getAmount();
        if (recentTransactions.containsKey(key)) {
            return new TransactionResult(TransactionStatus.ALERT, "Repeated transaction in short span.");
        }
        recentTransactions.put(key, String.valueOf(txn.getTimestamp()));
        return new TransactionResult(TransactionStatus.VALID, "No repetition.");
    }
}