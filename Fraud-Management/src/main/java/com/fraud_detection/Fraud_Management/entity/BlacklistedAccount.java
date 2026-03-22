package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_accounts")
@Data
public class BlacklistedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "reason")
    private String reason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    public BlacklistedAccount() {

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public BlacklistedAccount(Long id, String accountNumber, String reason, LocalDateTime blockedAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.reason = reason;
        this.blockedAt = blockedAt;
    }
}