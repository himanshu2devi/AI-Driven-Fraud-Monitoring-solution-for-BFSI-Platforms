package com.wipro.fraud.aiassistant.dto;

import lombok.Data;

@Data
public class AssistantRequest {

    // conversation memory identifier
    private String sessionId;

    // analyst asking the question
    private String analystId;

    // optional fraud transaction reference
    private String transactionId;

    // user question
    private String question;

}