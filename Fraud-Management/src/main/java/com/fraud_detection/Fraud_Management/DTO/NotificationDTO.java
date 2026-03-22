package com.fraud_detection.Fraud_Management.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class NotificationDTO {
    private String userId;
    private String accountNo;
    private String transactionStatus;

    public NotificationDTO(String userId, String accountNo, String transactionStatus) {
        this.userId = userId;
        this.accountNo = accountNo;
        this.transactionStatus = transactionStatus;
    }

    public NotificationDTO() {

    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public void setTransactionStatus(String transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
}