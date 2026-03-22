package com.fraud_detection.notification.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Fraud1KafkaMessage {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("account_no")
    private String accountNo;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    // No-argument constructor
    public Fraud1KafkaMessage() {
    }

    // Constructor with @JsonProperty on parameters
    @JsonCreator
    public Fraud1KafkaMessage(
            @JsonProperty("user_id") String userId,
            @JsonProperty("account_no") String accountNo,
            @JsonProperty("transaction_status") String transactionStatus) {
        this.userId = userId;
        this.accountNo = accountNo;
        this.transactionStatus = transactionStatus;
    }

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("user_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("account_no")
    public String getAccountNo() {
        return accountNo;
    }

    @JsonProperty("account_no")
    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    @JsonProperty("transaction_status")
    public String getTransactionStatus() {
        return transactionStatus;
    }

    @JsonProperty("transaction_status")
    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
}