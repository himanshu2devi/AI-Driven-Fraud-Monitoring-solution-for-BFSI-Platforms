package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

@Component
public class RoundAmountRule implements TransactionRule {

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        if (txn.getAmount() % 100000 == 0 && txn.getAmount() <= 1000000) {
            return new TransactionResult(TransactionStatus.ALERT, "Suspicious round amount transaction.");
        }
        return new TransactionResult(TransactionStatus.VALID, "Amount seems fine.");
    }
}