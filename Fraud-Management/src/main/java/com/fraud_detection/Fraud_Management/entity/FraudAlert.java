package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Data
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String accountNumber;

    @Column(name = "alert_message")
    private String alertMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public FraudAlert(Long id, String transactionId, String accountNumber, String alertMessage, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.alertMessage = alertMessage;
        this.createdAt = createdAt;
    }
    public FraudAlert(){}

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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
