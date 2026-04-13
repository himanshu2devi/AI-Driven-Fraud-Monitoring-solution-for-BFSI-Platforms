package com.fraud_detection.Fraud_Management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private String id;

    @Column(name = "status")
    private String status; // SUCCESSFUL / FAILED

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Transaction(String id, String status) {
        this.id = id;
        this.status = status;
    }

}
