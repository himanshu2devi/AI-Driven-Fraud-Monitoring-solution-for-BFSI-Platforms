package com.fraud_detection.Fraud_Management.DTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class TransactionDTO {

    @JsonCreator
    public TransactionDTO(@JsonProperty("transactionId") String transactionId,
                          @JsonProperty("transactionType") String transactionType,
                          @JsonProperty("accNoFrom") String accNoFrom,
                          @JsonProperty("accNoTo") String accNoTo,
                          @JsonProperty("amount") double amount,
                          @JsonProperty("currency") String currency,
                          @JsonProperty("timestamp") LocalDateTime timestamp,
                          @JsonProperty("sourceType") String sourceType,
                          @JsonProperty("userId") String userId) {
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.accNoFrom = accNoFrom;
        this.accNoTo = accNoTo;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.sourceType = sourceType;
        this.userId=userId;

    }

//    public TransactionDTO(String transactionId, String transactionType, String sourceType, Double amount, String currency, String timestamp, String accNoFrom, String accNoTo,String userId) {
//        this.transactionId = transactionId;
//        this.transactionType = transactionType;
//        this.sourceType = sourceType;
//        this.amount = amount;
//        this.currency = currency;
//        this.timestamp = timestamp;
//        this.accNoFrom = accNoFrom;
//        this.accNoTo = accNoTo;
//        this.userId= userId;
//    }


    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("transactionType")
    private String transactionType; // transfer, deposit, withdrawal

    @JsonProperty("sourceType")
    private String sourceType;      // debit_card, credit_card, bank

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("accNoFrom")
    private String accNoFrom;

    @JsonProperty("accNoTo")
    private String accNoTo;


    @JsonProperty("userId")
    private String userId;

    // Default constructor for Jackson deserialization
    public TransactionDTO() {
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAccNoFrom() {
        return accNoFrom;
    }

    public void setAccNoFrom(String accNoFrom) {
        this.accNoFrom = accNoFrom;
    }

    public String getAccNoTo() {
        return accNoTo;
    }

    public void setAccNoTo(String accNoTo) {
        this.accNoTo = accNoTo;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}



