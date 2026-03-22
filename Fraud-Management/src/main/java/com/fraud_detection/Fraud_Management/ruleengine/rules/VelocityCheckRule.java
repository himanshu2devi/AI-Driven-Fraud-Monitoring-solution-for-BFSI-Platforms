package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VelocityCheckRule implements TransactionRule {

    private final Map<String, Instant> lastTransactionTime = new ConcurrentHashMap<>();
    private static final long THRESHOLD_SECONDS = 5;

    @Override
    public TransactionResult apply(TransactionDTO transaction) {
        Instant now = Instant.now();
        String account = transaction.getAccNoFrom();

        if (account == null) {
            return new TransactionResult(TransactionStatus.ALERT, "No source account to check velocity");
        }

        Instant lastTime = lastTransactionTime.get(account);

        if (lastTime != null) {
            long secondsBetween = Math.abs(now.getEpochSecond() - lastTime.getEpochSecond());
            if (secondsBetween < THRESHOLD_SECONDS) {
                return new TransactionResult(TransactionStatus.FRAUD, "Multiple quick successive transactions detected");
            }
        }

        lastTransactionTime.put(account, now);
        return new TransactionResult(TransactionStatus.VALID, "Velocity check passed");
    }
}