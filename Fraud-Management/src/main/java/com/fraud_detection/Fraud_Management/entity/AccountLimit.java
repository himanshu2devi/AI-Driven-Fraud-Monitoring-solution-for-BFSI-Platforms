package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_limits")
@Data
@Builder
public class AccountLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "daily_limit", nullable = false)
    private Double dailyLimit;

    @Column(name = "transaction_limit", nullable = false)
    private Double transactionLimit;

    public AccountLimit(Long id, String accountNumber, Double dailyLimit, Double transactionLimit) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.dailyLimit = dailyLimit;
        this.transactionLimit = transactionLimit;
    }

    public AccountLimit() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Double getTransactionLimit() {
        return transactionLimit;
    }

    public void setTransactionLimit(Double transactionLimit) {
        this.transactionLimit = transactionLimit;
    }
}