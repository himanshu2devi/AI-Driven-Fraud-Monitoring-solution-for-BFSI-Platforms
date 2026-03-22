package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "suspicious_merchants")
@Data

@Builder
public class SuspiciousMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "reason", nullable = false)
    private String reason; // E.g., "fraudulent activities"

    public SuspiciousMerchant(Long id, String accountNumber, String reason) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.reason = reason;
    }

    public SuspiciousMerchant(){}


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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}