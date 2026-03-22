package com.fraud_detection.notification.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FraudKafkaMessage {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("account")
    private String accountNo;

    // No-argument constructor
    public FraudKafkaMessage() {
    }

    // All-argument constructor with @JsonProperty
    @JsonCreator
    public FraudKafkaMessage(
            @JsonProperty("transaction_id") String transactionId,
            @JsonProperty("reason") String reason,
            @JsonProperty("account_no") String accountNo) {
        this.transactionId = transactionId;
        this.reason = reason;
        this.accountNo = accountNo;
    }

    @JsonProperty("transaction_id")
    public String getTransactionId() {
        return transactionId;
    }

    @JsonProperty("transaction_id")
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

    @JsonProperty("account_no")
    public String getAccountNo() {
        return accountNo;
    }

    @JsonProperty("account_no")
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }
}