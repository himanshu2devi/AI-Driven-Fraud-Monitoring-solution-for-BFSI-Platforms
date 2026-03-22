package com.fraud_detection.notification.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransactionKafkaMessage {

    @JsonProperty("user_id")
    private Long user_id;

    @JsonProperty("status")
    private String status;

    public TransactionKafkaMessage(@JsonProperty("user_id") Long user_id,
                                   @JsonProperty("status") String status) {
        this.user_id = user_id;
        this.status = status;
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUserId(Long user_id) {
        this.user_id = user_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}