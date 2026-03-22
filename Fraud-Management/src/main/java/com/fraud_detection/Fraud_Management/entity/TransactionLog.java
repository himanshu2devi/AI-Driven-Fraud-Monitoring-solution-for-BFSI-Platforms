package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_logs")
@Data
@Builder
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="transaction_id")
    private String transactionId;

    @Column(name="account_from")
    private String accountFrom;

    @Column(name="account_to")
    private String accountTo;

    @Column(name="amount")
    private Double amount;

    @Column(name="transaction_type")
    private String transactionType;

    @Column(name="status")
    private String status; // VALID, ALERT, FRAUD
    @Column(name="reason")
    private String reason;

    @Column(name="timestamp")
    private LocalDateTime timestamp;

    public TransactionLog(Long id, String transactionId, String accountFrom, String accountTo, Double amount, String transactionType, String status, String reason, LocalDateTime timestamp) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountFrom = accountFrom;
        this.accountTo = accountTo;
        this.amount = amount;
        this.transactionType = transactionType;
        this.status = status;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public TransactionLog(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountFrom() {
        return accountFrom;
    }

    public void setAccountFrom(String accountFrom) {
        this.accountFrom = accountFrom;
    }

    public String getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(String accountTo) {
        this.accountTo = accountTo;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}