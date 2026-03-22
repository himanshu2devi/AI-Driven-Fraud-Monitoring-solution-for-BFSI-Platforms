package com.wipro.fraud.aiassistant.service;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;

public interface FraudAssistantService {

    AssistantResponse processQuery(AssistantRequest request);

}