package com.fraud_detection.Fraud_Management.ruleengine;


public class ResultHolder {
    private TransactionStatus status;
    private String reason;

    public ResultHolder() {
        this.status = TransactionStatus.VALID;
        this.reason = "Initial state";
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
