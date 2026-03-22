package com.fraud_detection.Fraud_Management.ruleengine;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;

public interface TransactionRule {
    TransactionResult apply(TransactionDTO transaction);
}