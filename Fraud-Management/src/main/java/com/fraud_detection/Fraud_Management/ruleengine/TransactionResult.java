package com.fraud_detection.Fraud_Management.ruleengine;


public class TransactionResult {
    private final TransactionStatus status;
    private final String reason;

    public TransactionResult(TransactionStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}