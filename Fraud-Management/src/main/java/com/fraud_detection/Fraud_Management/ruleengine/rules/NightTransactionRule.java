package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class NightTransactionRule implements TransactionRule {

    @Override
    public TransactionResult apply(TransactionDTO transaction) {
        String timestampStr = String.valueOf(transaction.getTimestamp());

        if (timestampStr == null || timestampStr.isEmpty()) {
            return new TransactionResult(TransactionStatus.VALID, "Timestamp is missing");
        }

        try {
            Instant instant = Instant.parse(timestampStr);
            LocalDateTime txnTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            int hour = txnTime.getHour();

            if (hour >= 0 && hour < 4) {
                return new TransactionResult(TransactionStatus.ALERT, "Transaction during suspicious night hours");
            }

            return new TransactionResult(TransactionStatus.VALID, "Transaction time is normal");

        } catch (Exception e) {
            return new TransactionResult(TransactionStatus.VALID, "Invalid timestamp format");
        }
    }
}