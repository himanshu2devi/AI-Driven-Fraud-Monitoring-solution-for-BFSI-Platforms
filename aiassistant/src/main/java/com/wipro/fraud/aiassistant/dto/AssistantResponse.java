package com.wipro.fraud.aiassistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssistantResponse {

    private String answer;

    private String source;

}