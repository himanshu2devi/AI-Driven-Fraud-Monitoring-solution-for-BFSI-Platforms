package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class WeekendTransactionRule implements TransactionRule {

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        try {
            LocalDate date = LocalDate.parse(String.valueOf( txn.getTimestamp()).substring(0, 10));
            DayOfWeek day = date.getDayOfWeek();

            if ((day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) && txn.getAmount() > 500000) {
                return new TransactionResult(TransactionStatus.ALERT, "High-value weekend transaction.");
            }

        } catch (Exception e) {
            return new TransactionResult(TransactionStatus.VALID, "Error parsing date.");
        }

        return new TransactionResult(TransactionStatus.VALID, "Not a risky weekend transaction.");
    }
}