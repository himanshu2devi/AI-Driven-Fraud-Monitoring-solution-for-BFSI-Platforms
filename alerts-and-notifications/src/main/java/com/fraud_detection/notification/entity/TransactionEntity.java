package com.fraud_detection.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@NoArgsConstructor
@Builder
@Getter
public class TransactionEntity {

    @Id
    @Column(name= "id")
    private String id;

    @Column(name= "type")
    private String type;

    @Column(name= "userId")
    private Long userId;

    @Column(name= "accNoFrom")
    private String accNoFrom;

    @Column(name= "accNoTo")
    private String accNoTo;

    @Column(name= "status")
    private String status;

    @Column(name= "timestamp")
    private LocalDateTime timestamp;

    public TransactionEntity(String id, String type, Long userId, String accNoFrom, String accNoTo, String status, LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.userId = userId;
        this.accNoFrom = accNoFrom;
        this.accNoTo = accNoTo;
        this.status = status;
        this.timestamp = timestamp;
    }

    public TransactionEntity(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
