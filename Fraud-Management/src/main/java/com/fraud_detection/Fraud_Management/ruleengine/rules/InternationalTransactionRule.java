package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

@Component
public class InternationalTransactionRule implements TransactionRule {

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        if (!"INR".equalsIgnoreCase(txn.getCurrency()) && txn.getAmount() > 100000) {
            return new TransactionResult(TransactionStatus.ALERT, "Large international transaction.");
        }
        return new TransactionResult(TransactionStatus.VALID, "Domestic or safe international transaction.");
    }
}
