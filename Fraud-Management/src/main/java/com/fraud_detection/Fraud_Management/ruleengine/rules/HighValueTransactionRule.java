package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.ResultHolder;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.jeasy.rules.annotation.*;

@Rule(name = "High Value Transaction", description = "Flag transactions over 10L as fraud")
public class HighValueTransactionRule {

    @Condition
    public boolean isHighValue(@Fact("transaction") TransactionDTO txn) {
        return txn.getAmount() > 1000000;
    }

    @Action
    public void markFraud(@Fact("resultHolder") ResultHolder resultHolder) {
        resultHolder.setStatus(TransactionStatus.FRAUD);
        resultHolder.setReason("High value transaction over 10L");
    }
}