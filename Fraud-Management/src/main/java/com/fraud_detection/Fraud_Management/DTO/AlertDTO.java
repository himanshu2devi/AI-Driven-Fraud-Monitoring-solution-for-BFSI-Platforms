package com.fraud_detection.Fraud_Management.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlertDTO {
    private String transactionId;
    private String reason;
    private String account;

    // Default constructor
    public AlertDTO() {
    }

    // Constructor with @JsonProperty annotations for each field
    public AlertDTO(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("reason") String reason,
            @JsonProperty("account") String account
    ) {
        this.transactionId = transactionId;
        this.reason = reason;
        this.account = account;
    }

    // Getters and setters
    @JsonProperty("transactionId")
    public String getTransactionId() {
        return transactionId;
    }

    @JsonProperty("transactionId")
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @JsonProperty("reason")
    public String getReason() {
        return reason;
    }

    @JsonProperty("reason")
    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonProperty("account")
    public String getAccount() {
        return account;
    }

    @JsonProperty("account")
    public void setAccount(String account) {
        this.account = account;
    }
}